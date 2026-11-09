package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyit.core.Collections
import com.studyit.core.snapshots
import com.studyit.core.toNotificationOrNull
import com.studyit.data.model.AppNotification
import com.studyit.data.model.NotificationType
import com.studyit.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun collection() = firestore.collection(Collections.NOTIFICATIONS)

    fun observe(uid: String, limit: Long = 60L): Flow<List<AppNotification>> =
        collection()
            .whereEqualTo("recipientId", uid)
            .limit(limit)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toNotificationOrNull() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeUnreadCount(uid: String): Flow<Int> =
        observe(uid).map { list -> list.count { !it.isRead } }

    suspend fun push(
        recipientId: String,
        sender: User?,
        type: NotificationType,
        postId: String?,
        message: String
    ) {
        if (recipientId.isBlank() || recipientId == sender?.uid) return
        val notification = AppNotification(
            recipientId = recipientId,
            senderId = sender?.uid ?: "",
            senderName = sender?.displayName ?: "Studit",
            type = type.name,
            postId = postId ?: "",
            message = message,
            isRead = false,
            createdAt = Timestamp.now()
        )
        collection().add(notification).await()
    }

    suspend fun markRead(notificationId: String) {
        collection().document(notificationId).update("isRead", true).await()
    }

    suspend fun markAllRead(uid: String) {
        val unread = collection()
            .whereEqualTo("recipientId", uid)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        if (unread.isEmpty) return
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
    }

    suspend fun clearAll(uid: String) {
        val all = collection()
            .whereEqualTo("recipientId", uid)
            .limit(200)
            .get()
            .await()
        if (all.isEmpty) return
        val batch = firestore.batch()
        all.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
