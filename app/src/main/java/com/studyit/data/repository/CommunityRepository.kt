package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.core.snapshots
import com.studyit.core.toCommunityOrNull
import com.studyit.data.model.Community
import com.studyit.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class CommunityRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun communities() = firestore.collection(Collections.COMMUNITIES)
    private fun members() = firestore.collection(Collections.COMMUNITY_MEMBERS)

    fun memberDocId(communityId: String, uid: String) = communityId + "_" + uid

    fun observeCommunities(): Flow<List<Community>> =
        communities().limit(200).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toCommunityOrNull() }
                .sortedByDescending { it.memberCount }
        }

    fun observeCommunity(communityId: String): Flow<Community?> =
        communities().document(communityId).snapshots().map { it.toCommunityOrNull() }

    suspend fun getCommunity(communityId: String): Community? =
        runCatching { communities().document(communityId).get().await().toCommunityOrNull() }
            .getOrNull()

    fun observeMembership(communityId: String, uid: String): Flow<Boolean> =
        members().document(memberDocId(communityId, uid)).snapshots().map { it.exists() }

    fun observeMyCommunities(uid: String): Flow<List<String>> =
        members().whereEqualTo("uid", uid).limit(200).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.getString("communityId") }
        }

    suspend fun searchCommunities(query: String, limit: Long = 20L): List<Community> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return runCatching {
            communities().orderBy("nameLower")
                .startAt(q)
                .endAt(q + "\uF8FF")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toCommunityOrNull() }
        }.getOrDefault(emptyList())
    }

    suspend fun join(community: Community, user: User): Result<Unit> = runCatching {
        val docRef = members().document(memberDocId(community.communityId, user.uid))
        if (docRef.get().await().exists()) return@runCatching Unit
        docRef.set(
            mapOf(
                "membershipId" to docRef.id,
                "communityId" to community.communityId,
                "uid" to user.uid,
                "role" to "member",
                "joinedAt" to Timestamp.now()
            )
        ).await()
        communities().document(community.communityId)
            .update("memberCount", community.memberCount + 1).await()
        Unit
    }

    suspend fun leave(community: Community, user: User): Result<Unit> = runCatching {
        val docRef = members().document(memberDocId(community.communityId, user.uid))
        if (!docRef.get().await().exists()) return@runCatching Unit
        docRef.delete().await()
        communities().document(community.communityId)
            .update("memberCount", (community.memberCount - 1).coerceAtLeast(0L)).await()
        Unit
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        rules: List<String>,
        creator: User
    ): Result<String> = runCatching {
        val cleanName = name.trim().removePrefix("r/")
        if (cleanName.length < 3) throw IllegalStateException("Community name is too short")
        val existing = communities()
            .whereEqualTo("nameLower", cleanName.lowercase())
            .limit(1)
            .get()
            .await()
        if (!existing.isEmpty) throw IllegalStateException("That community already exists")

        val document = communities().document()
        document.set(
            Community(
                communityId = document.id,
                name = cleanName,
                nameLower = cleanName.lowercase(),
                description = description.trim(),
                creatorId = creator.uid,
                memberCount = 1L,
                rules = rules.filter { it.isNotBlank() },
                createdAt = Timestamp.now()
            )
        ).await()

        val memberRef = members().document(memberDocId(document.id, creator.uid))
        memberRef.set(
            mapOf(
                "membershipId" to memberRef.id,
                "communityId" to document.id,
                "uid" to creator.uid,
                "role" to "moderator",
                "joinedAt" to Timestamp.now()
            )
        ).await()
        document.id
    }
}
