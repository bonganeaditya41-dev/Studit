package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyit.core.Collections
import com.studyit.core.PointsConfig
import com.studyit.core.snapshots
import com.studyit.core.toPostOrNull
import com.studyit.data.model.FeedPage
import com.studyit.data.model.FeedSort
import com.studyit.data.model.NotificationType
import com.studyit.data.model.Post
import com.studyit.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val pointsRepository: PointsRepository,
    private val notificationRepository: NotificationRepository
) {

    private fun posts() = firestore.collection(Collections.POSTS)

    fun observePost(postId: String): Flow<Post?> =
        posts().document(postId).snapshots().map { it.toPostOrNull() }

    suspend fun getPost(postId: String): Post? =
        runCatching { posts().document(postId).get().await().toPostOrNull() }.getOrNull()

    suspend fun createPost(
        author: User,
        communityId: String,
        communityName: String,
        title: String,
        description: String,
        subject: String,
        academicClass: String,
        tags: List<String>,
        imageUrl: String
    ): Result<String> = runCatching {
        val now = Timestamp.now()
        val document = posts().document()
        val post = Post(
            postId = document.id,
            authorId = author.uid,
            authorName = author.displayName,
            authorUsername = author.username,
            authorPhotoUrl = author.photoUrl,
            communityId = communityId,
            communityName = communityName,
            title = title.trim(),
            titleLower = title.trim().lowercase(),
            description = description.trim(),
            imageUrl = imageUrl,
            subject = subject,
            academicClass = academicClass,
            tags = tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct(),
            score = 0L,
            trendScore = 1L,
            isAnswered = false,
            createdAt = now,
            updatedAt = now
        )
        document.set(post).await()
        pointsRepository.awardPoints(author.uid, PointsConfig.QUESTION_CREATED, "question_created")
        pointsRepository.bumpCounter(author.uid, "questionsCount", 1L)
        document.id
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        subject: String,
        tags: List<String>,
        imageUrl: String
    ): Result<Unit> = runCatching {
        posts().document(postId).update(
            mapOf(
                "title" to title.trim(),
                "titleLower" to title.trim().lowercase(),
                "description" to description.trim(),
                "subject" to subject,
                "tags" to tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct(),
                "imageUrl" to imageUrl,
                "updatedAt" to Timestamp.now()
            )
        ).await()
        Unit
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        posts().document(postId).delete().await()
        Unit
    }

    suspend fun feed(
        sort: FeedSort,
        communityId: String?,
        pageSize: Long,
        lastDocument: DocumentSnapshot?
    ): FeedPage {
        if (communityId != null) {
            val snapshot = posts()
                .whereEqualTo("communityId", communityId)
                .limit(100)
                .get()
                .await()
            val posts = snapshot.documents.mapNotNull { it.toPostOrNull() }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            return FeedPage(posts = posts, lastDocument = null, endReached = true)
        }

        var query: Query = posts()
        when (sort) {
            FeedSort.LATEST -> query = query.orderBy("createdAt", Query.Direction.DESCENDING)
            FeedSort.POPULAR -> query = query.orderBy("score", Query.Direction.DESCENDING)
            FeedSort.TRENDING -> query = query.orderBy("trendScore", Query.Direction.DESCENDING)
            FeedSort.UNANSWERED -> query = query.whereEqualTo("isAnswered", false)
        }
        if (lastDocument != null) query = query.startAfter(lastDocument)
        val snapshot = query.limit(pageSize).get().await()
        return FeedPage(
            posts = snapshot.documents.mapNotNull { it.toPostOrNull() },
            lastDocument = snapshot.documents.lastOrNull(),
            endReached = snapshot.documents.size < pageSize
        )
    }

    suspend fun userPosts(uid: String, limit: Long = 30L): List<Post> = runCatching {
        posts().whereEqualTo("authorId", uid)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toPostOrNull() }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }.getOrDefault(emptyList())

    suspend fun searchPosts(query: String, limit: Long = 20L): List<Post> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return runCatching {
            posts().orderBy("titleLower")
                .startAt(q)
                .endAt(q + "\uF8FF")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toPostOrNull() }
        }.getOrDefault(emptyList())
    }

    suspend fun notifyAnswer(post: Post, answerAuthor: User, answerId: String) {
        notificationRepository.push(
            recipientId = post.authorId,
            sender = answerAuthor,
            type = NotificationType.ANSWER,
            postId = post.postId,
            message = answerAuthor.displayName + " answered your question \"" + post.title.take(60) + "\""
        )
    }
}
