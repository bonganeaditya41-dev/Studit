package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyit.core.Collections
import com.studyit.core.snapshots
import com.studyit.core.toUserOrNull
import com.studyit.data.model.LeaderboardEntry
import com.studyit.data.model.User
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun users() = firestore.collection(Collections.USERS)

    fun observeUser(uid: String): Flow<User?> =
        users().document(uid).snapshots().map { it.toUserOrNull() }

    suspend fun getUser(uid: String): User? =
        runCatching { users().document(uid).get().await().toUserOrNull() }.getOrNull()

    suspend fun getUsers(ids: List<String>): List<User> =
        if (ids.isEmpty()) emptyList()
        else runCatching {
            ids.chunked(10).flatMap { chunk ->
                users().whereIn("uid", chunk).get().await().documents.mapNotNull { it.toUserOrNull() }
            }
        }.getOrDefault(emptyList())

    suspend fun updateProfile(
        uid: String,
        fullName: String,
        bio: String,
        academicClass: String,
        photoUrl: String
    ): Result<Unit> = runCatching {
        users().document(uid).update(
            mapOf(
                "fullName" to fullName.trim(),
                "bio" to bio.trim(),
                "academicClass" to academicClass,
                "photoUrl" to photoUrl
            )
        ).await()
        Unit
    }

    suspend fun updatePrivacy(uid: String, isPublic: Boolean): Result<Unit> = runCatching {
        users().document(uid).update("isProfilePublic", isPublic).await()
        Unit
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        runCatching { users().document(uid).update("fcmToken", token).await() }
    }

    suspend fun searchUsers(query: String, limit: Long = 20L): List<User> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return runCatching {
            users().orderBy("usernameLower")
                .startAt(q)
                .endAt(q + "\uF8FF")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toUserOrNull() }
        }.getOrDefault(emptyList())
    }

    suspend fun allTimeLeaderboard(limit: Long = 50L): List<User> = runCatching {
        users().orderBy("totalPoints", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toUserOrNull() }
    }.getOrDefault(emptyList())

    /**
     * Weekly / monthly boards are aggregated from the pointsEvents ledger, so no
     * scheduled reset job (Cloud Functions) is needed on the free plan.
     */
    suspend fun windowedLeaderboard(since: Timestamp, limit: Long = 400L): List<LeaderboardEntry> {
        val events = runCatching {
            firestore.collection(Collections.POINTS_EVENTS)
                .whereGreaterThan("createdAt", since)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: return@mapNotNull null
                    uid to (doc.getLong("delta") ?: 0L)
                }
        }.getOrDefault(emptyList())

        if (events.isEmpty()) return emptyList()

        val totals = events.groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sum() }
            .entries
            .sortedByDescending { it.value }
            .take(20)

        val profiles = getUsers(totals.map { it.key }).associateBy { it.uid }
        return totals.mapIndexedNotNull { index, entry ->
            val user = profiles[entry.key] ?: return@mapIndexedNotNull null
            LeaderboardEntry(user = user, points = entry.value, position = index + 1)
        }
    }

    fun startOf(range: com.studyit.data.model.LeaderboardRange): Timestamp {
        val calendar = Calendar.getInstance()
        when (range) {
            com.studyit.data.model.LeaderboardRange.WEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            com.studyit.data.model.LeaderboardRange.MONTHLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
            }
            com.studyit.data.model.LeaderboardRange.ALL_TIME -> {
                calendar.add(Calendar.YEAR, -50)
            }
        }
        return Timestamp(calendar.time)
    }
}
