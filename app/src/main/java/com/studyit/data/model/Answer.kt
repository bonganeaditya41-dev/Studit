package com.studyit.data.model

import com.google.firebase.Timestamp

data class Answer(
    val answerId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val upvotesCount: Long = 0L,
    val downvotesCount: Long = 0L,
    val score: Long = 0L,
    val isAccepted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val answerId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val parentCommentId: String = "",
    val createdAt: Timestamp? = null
)
