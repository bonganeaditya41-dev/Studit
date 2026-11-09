package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.data.model.PointsEvent
import com.studyit.data.model.Rank
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Client-side points engine.
 *
 * NOTE: the free (Spark) Firebase plan does not support Cloud Functions, so point
 * awards happen on the client. firestore.rules keeps this bounded: a single write may
 * raise totalPoints by at most 30 and the username / uid can never be rewritten.
 * For a fully tamper-proof ledger, move awardPoints() to a Cloud Function later.
 */
@Singleton
class PointsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun awardPoints(uid: String, delta: Int, reason: String) {
        if (uid.isBlank() || delta == 0) return
        val userRef = firestore.collection(Collections.USERS).document(uid)
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) return
        val current = snapshot.getLong("totalPoints") ?: 0L
        val updated = (current + delta).coerceAtLeast(0L)
        val batch = firestore.batch()
        batch.update(
            userRef,
            mapOf(
                "totalPoints" to updated,
                "rank" to Rank.fromPoints(updated.toInt()).label
            )
        )
        batch.set(
            firestore.collection(Collections.POINTS_EVENTS).document(),
            PointsEvent(
                uid = uid,
                delta = delta.toLong(),
                reason = reason,
                createdAt = Timestamp.now()
            )
        )
        batch.commit().await()
    }

    suspend fun bumpCounter(uid: String, field: String, delta: Long) {
        if (uid.isBlank() || delta == 0L) return
        val userRef = firestore.collection(Collections.USERS).document(uid)
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) return
        val current = snapshot.getLong(field) ?: 0L
        userRef.update(field, (current + delta).coerceAtLeast(0L)).await()
    }
}
