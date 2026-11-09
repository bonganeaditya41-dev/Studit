package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.core.PointsConfig
import com.studyit.core.snapshots
import com.studyit.data.model.NotificationType
import com.studyit.data.model.User
import com.studyit.data.model.VoteTargetType
import com.studyit.data.model.VoteType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class VoteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val pointsRepository: PointsRepository,
    private val notificationRepository: NotificationRepository
) {

    private fun votes() = firestore.collection(Collections.VOTES)

    fun voteDocId(targetType: VoteTargetType, targetId: String, uid: String): String =
        targetType.value + "_" + targetId + "_" + uid

    fun observeMyVote(
        targetType: VoteTargetType,
        targetId: String,
        uid: String
    ): Flow<VoteType?> = votes().document(voteDocId(targetType, targetId, uid))
        .snapshots()
        .map { snapshot ->
            when (snapshot.getString("voteType")) {
                VoteType.UP.name -> VoteType.UP
                VoteType.DOWN.name -> VoteType.DOWN
                else -> null
            }
        }

    /**
     * Applies (or removes) the caller's vote. Passing the currently selected vote
     * toggles it off. One vote document per user per target keeps this idempotent.
     */
    suspend fun vote(
        targetType: VoteTargetType,
        targetId: String,
        authorId: String,
        currentVote: VoteType?,
        desiredVote: VoteType?,
        actor: User
    ): Result<Unit> = runCatching {
        val newVote = if (currentVote == desiredVote) null else desiredVote
        if (newVote != null && authorId == actor.uid) {
            throw IllegalStateException("You cannot vote on your own contribution")
        }

        val collectionName = when (targetType) {
            VoteTargetType.POST -> Collections.POSTS
            VoteTargetType.ANSWER -> Collections.ANSWERS
        }
        val targetRef = firestore.collection(collectionName).document(targetId)
        val targetSnapshot = targetRef.get().await()
        if (!targetSnapshot.exists()) throw IllegalStateException("This post is no longer available")

        var up = targetSnapshot.getLong("upvotesCount") ?: 0L
        var down = targetSnapshot.getLong("downvotesCount") ?: 0L

        when (currentVote) {
            VoteType.UP -> up = (up - 1).coerceAtLeast(0L)
            VoteType.DOWN -> down = (down - 1).coerceAtLeast(0L)
            null -> Unit
        }
        when (newVote) {
            VoteType.UP -> up += 1
            VoteType.DOWN -> down += 1
            null -> Unit
        }

        val scoreDelta = (up - down) - ((targetSnapshot.getLong("upvotesCount") ?: 0L) -
            (targetSnapshot.getLong("downvotesCount") ?: 0L))
        val trendDelta = scoreDelta + if (newVote == VoteType.UP) 1 else 0

        val counterUpdates = mutableMapOf<String, Any>(
            "upvotesCount" to up,
            "downvotesCount" to down,
            "score" to (up - down),
            "updatedAt" to Timestamp.now()
        )
        if (targetType == VoteTargetType.POST) {
            counterUpdates["trendScore"] =
                (targetSnapshot.getLong("trendScore") ?: 0L) + trendDelta
        }

        val batch = firestore.batch()
        batch.update(targetRef, counterUpdates)

        val voteRef = votes().document(voteDocId(targetType, targetId, actor.uid))
        if (newVote == null) {
            batch.delete(voteRef)
        } else {
            batch.set(
                voteRef,
                mapOf(
                    "voteId" to voteRef.id,
                    "targetId" to targetId,
                    "targetType" to targetType.value,
                    "userId" to actor.uid,
                    "voteType" to newVote.name,
                    "createdAt" to Timestamp.now()
                )
            )
        }
        batch.commit().await()

        val perUp = when (targetType) {
            VoteTargetType.POST -> PointsConfig.QUESTION_UPVOTE
            VoteTargetType.ANSWER -> PointsConfig.ANSWER_UPVOTE
        }
        val perDown = when (targetType) {
            VoteTargetType.POST -> PointsConfig.QUESTION_DOWNVOTE
            VoteTargetType.ANSWER -> PointsConfig.ANSWER_DOWNVOTE
        }
        val previousPoints = when (currentVote) {
            VoteType.UP -> perUp
            VoteType.DOWN -> perDown
            null -> 0
        }
        val newPoints = when (newVote) {
            VoteType.UP -> perUp
            VoteType.DOWN -> perDown
            null -> 0
        }
        val pointsDelta = newPoints - previousPoints
        if (pointsDelta != 0) {
            pointsRepository.awardPoints(authorId, pointsDelta, "vote_" + targetType.value.lowercase())
            if (pointsDelta > 0) {
                pointsRepository.bumpCounter(authorId, "upvotesReceived", 1L)
                notificationRepository.push(
                    recipientId = authorId,
                    sender = actor,
                    type = NotificationType.UPVOTE,
                    postId = if (targetType == VoteTargetType.POST) targetId else "",
                    message = actor.displayName + " upvoted your " +
                        if (targetType == VoteTargetType.POST) "question" else "answer"
                )
            }
        }
        Unit
    }

    fun observeMyVotes(uid: String): Flow<Map<String, VoteType>> =
        votes().whereEqualTo("userId", uid).limit(300).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val targetId = doc.getString("targetId") ?: return@mapNotNull null
                val type = when (doc.getString("voteType")) {
                    VoteType.UP.name -> VoteType.UP
                    VoteType.DOWN.name -> VoteType.DOWN
                    else -> null
                }
                if (type == null) null else targetId to type
            }.toMap()
        }
}
