package com.studyit.core

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.studyit.data.model.Answer
import com.studyit.data.model.AppNotification
import com.studyit.data.model.Comment
import com.studyit.data.model.Community
import com.studyit.data.model.Post
import com.studyit.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Query.snapshots(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.snapshots(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentSnapshot.toUserOrNull(): User? =
    try {
        toObject(User::class.java)?.copy(uid = id)
    } catch (e: Exception) {
        null
    }

fun DocumentSnapshot.toPostOrNull(): Post? =
    try {
        toObject(Post::class.java)?.copy(postId = id)
    } catch (e: Exception) {
        null
    }

fun DocumentSnapshot.toAnswerOrNull(): Answer? =
    try {
        toObject(Answer::class.java)?.copy(answerId = id)
    } catch (e: Exception) {
        null
    }

fun DocumentSnapshot.toCommentOrNull(): Comment? =
    try {
        toObject(Comment::class.java)?.copy(commentId = id)
    } catch (e: Exception) {
        null
    }

fun DocumentSnapshot.toCommunityOrNull(): Community? =
    try {
        toObject(Community::class.java)?.copy(communityId = id)
    } catch (e: Exception) {
        null
    }

fun DocumentSnapshot.toNotificationOrNull(): AppNotification? =
    try {
        toObject(AppNotification::class.java)?.copy(notificationId = id)
    } catch (e: Exception) {
        null
    }
