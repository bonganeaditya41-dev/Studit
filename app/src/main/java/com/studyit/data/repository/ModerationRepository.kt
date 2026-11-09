package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.data.model.Report
import com.studyit.data.model.ReportTargetType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class ModerationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun reports() = firestore.collection(Collections.REPORTS)
    private fun blocked(uid: String) =
        firestore.collection(Collections.USERS).document(uid).collection("blocked")

    suspend fun report(
        reporterId: String,
        targetId: String,
        targetType: ReportTargetType,
        reason: String
    ): Result<Unit> = runCatching {
        if (reason.isBlank()) throw IllegalStateException("Please describe the problem")
        reports().add(
            Report(
                reporterId = reporterId,
                targetId = targetId,
                targetType = targetType.name,
                reason = reason.trim(),
                status = "open",
                createdAt = Timestamp.now()
            )
        ).await()
        Unit
    }

    suspend fun blockUser(uid: String, blockedUid: String): Result<Unit> = runCatching {
        blocked(uid).document(blockedUid).set(
            mapOf("blockedUid" to blockedUid, "createdAt" to Timestamp.now())
        ).await()
        Unit
    }

    suspend fun unblockUser(uid: String, blockedUid: String): Result<Unit> = runCatching {
        blocked(uid).document(blockedUid).delete().await()
        Unit
    }

    suspend fun blockedUsers(uid: String): List<String> = runCatching {
        blocked(uid).get().await().documents.map { it.id }
    }.getOrDefault(emptyList())
}
