package com.studyit.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val communityId: String = "",
    val communityName: String = "",
    val title: String = "",
    val titleLower: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val subject: String = "",
    val academicClass: String = "",
    val tags: List<String> = emptyList(),
    val upvotesCount: Long = 0L,
    val downvotesCount: Long = 0L,
    val answersCount: Long = 0L,
    val score: Long = 0L,
    val trendScore: Long = 0L,
    val isAnswered: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FeedPage(
    val posts: List<Post> = emptyList(),
    val lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null,
    val endReached: Boolean = false
)
