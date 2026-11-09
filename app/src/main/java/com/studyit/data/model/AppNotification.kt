package com.studyit.data.model

import com.google.firebase.Timestamp

data class AppNotification(
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = NotificationType.ANNOUNCEMENT.name,
    val postId: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
