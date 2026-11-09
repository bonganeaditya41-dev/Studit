package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.core.PointsConfig
import com.studyit.core.snapshots
import com.studyit.core.toAnswerOrNull
import com.studyit.core.toCommentOrNull
import com.studyit.data.model.Answer
import com.studyit.data.model.Comment
import com.studyit.data.model.NotificationType
import com.studyit.data.model.Post
import com.studyit.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class AnswerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val pointsRepository: PointsRepository,
    private val notificationRepository: NotificationRepository
) {

    private fun answers() = firestore.collection(Collections.ANSWERS)
    private fun comments() = firestore.collection(Collections.COMMENTS)

    fun observeAnswers(postId: String): Flow<List<Answer>> =
        answers().whereEqualTo("postId", postId).limit(200).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toAnswerOrNull() }
        }

    fun observeComments(postId: String): Flow<List<Comment>> =
        comments().whereEqualTo("postId", postId).limit(300).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toCommentOrNull() }
                .sortedBy { it.createdAt?.seconds ?: 0L }
        }

    suspend fun addAnswer(post: Post, author: User, content: String): Result<Unit> = runCatching {
        val now = Timestamp.now()
        val document = answers().document()
        document.set(
            Answer(
                answerId = document.id,
                postId = post.postId,
                authorId = author.uid,
                authorName = author.displayName,
                authorUsername = author.username,
                authorPhotoUrl = author.photoUrl,
                content = content.trim(),
                createdAt = now,
                updatedAt = now
            )
        ).await()

        val postRef = firestore.collection(Collections.POSTS).document(post.postId)
        val snapshot = postRef.get().await()
        val count = (snapshot.getLong("answersCount") ?: 0L) + 1
        val trend = (snapshot.getLong("trendScore") ?: 0L) + 3
        postRef.update(
            mapOf(
                "answersCount" to count,
                "isAnswered" to true,
                "trendScore" to trend,
                "updatedAt" to now
            )
        ).await()

        pointsRepository.bumpCounter(author.uid, "answersCount", 1L)
        notificationRepository.push(
            recipientId = post.authorId,
            sender = author,
            type = NotificationType.ANSWER,
            postId = post.postId,
            message = author.displayName + " answered your question: " + post.title.take(50)
        )
        Unit
    }

    suspend fun updateAnswer(answerId: String, content: String): Result<Unit> = runCatching {
        answers().document(answerId).update(
            mapOf("content" to content.trim(), "updatedAt" to Timestamp.now())
        ).await()
        Unit
    }

    suspend fun deleteAnswer(answer: Answer): Result<Unit> = runCatching {
        answers().document(answer.answerId).delete().await()
        val postRef = firestore.collection(Collections.POSTS).document(answer.postId)
        val snapshot = postRef.get().await()
        if (snapshot.exists()) {
            val count = ((snapshot.getLong("answersCount") ?: 1L) - 1).coerceAtLeast(0L)
            postRef.update(
                mapOf("answersCount" to count, "isAnswered" to (count > 0))
            ).await()
        }
        pointsRepository.bumpCounter(answer.authorId, "answersCount", -1L)
        Unit
    }

    suspend fun acceptAnswer(post: Post, answer: Answer, actor: User): Result<Unit> = runCatching {
        if (post.authorId != actor.uid) {
            throw IllegalStateException("Only the person who asked can accept an answer")
        }
        val siblings = answers().whereEqualTo("postId", post.postId).get().await()
        val batch = firestore.batch()
        siblings.documents.forEach { doc ->
            if (doc.getBoolean("isAccepted") == true) {
                batch.update(doc.reference, "isAccepted", false)
            }
        }
        batch.update(answers().document(answer.answerId), "isAccepted", true)
        batch.commit().await()

        if (!answer.isAccepted) {
            pointsRepository.awardPoints(
                answer.authorId,
                PointsConfig.ANSWER_ACCEPTED,
                "answer_accepted"
            )
            pointsRepository.bumpCounter(answer.authorId, "acceptedAnswersCount", 1L)
            notificationRepository.push(
                recipientId = answer.authorId,
                sender = actor,
                type = NotificationType.ACCEPTED,
                postId = post.postId,
                message = "Your answer was accepted on \"" + post.title.take(50) + "\""
            )
        }
        Unit
    }

    suspend fun addComment(
        post: Post,
        answerId: String,
        parentCommentId: String,
        author: User,
        content: String
    ): Result<Unit> = runCatching {
        val document = comments().document()
        document.set(
            Comment(
                commentId = document.id,
                postId = post.postId,
                answerId = answerId,
                authorId = author.uid,
                authorName = author.displayName,
                authorPhotoUrl = author.photoUrl,
                content = content.trim(),
                parentCommentId = parentCommentId,
                createdAt = Timestamp.now()
            )
        ).await()

        if (answerId.isNotBlank()) {
            val answerSnapshot = answers().document(answerId).get().await()
            val answerAuthor = answerSnapshot.getString("authorId") ?: ""
            notificationRepository.push(
                recipientId = answerAuthor,
                sender = author,
                type = NotificationType.REPLY,
                postId = post.postId,
                message = author.displayName + " replied to an answer"
            )
        }
        Unit
    }

    suspend fun deleteComment(comment: Comment): Result<Unit> = runCatching {
        comments().document(comment.commentId).delete().await()
        Unit
    }
}
