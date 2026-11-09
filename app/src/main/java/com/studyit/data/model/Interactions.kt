package com.studyit.data.model

import com.google.firebase.Timestamp

data class Vote(
    val voteId: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val userId: String = "",
    val voteType: String = "",
    val createdAt: Timestamp? = null
)

data class Bookmark(
    val bookmarkId: String = "",
    val userId: String = "",
    val postId: String = "",
    val createdAt: Timestamp? = null
)

data class Report(
    val reportId: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val reason: String = "",
    val status: String = "open",
    val createdAt: Timestamp? = null
)

data class PointsEvent(
    val eventId: String = "",
    val uid: String = "",
    val delta: Long = 0L,
    val reason: String = "",
    val createdAt: Timestamp? = null
)
