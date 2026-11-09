package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.core.snapshots
import com.studyit.core.toPostOrNull
import com.studyit.data.model.Post
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class BookmarkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun bookmarks() = firestore.collection(Collections.BOOKMARKS)

    fun bookmarkDocId(uid: String, postId: String) = uid + "_" + postId

    fun isBookmarked(uid: String, postId: String): Flow<Boolean> =
        bookmarks().document(bookmarkDocId(uid, postId)).snapshots().map { it.exists() }

    fun observeBookmarkedPostIds(uid: String): Flow<List<String>> =
        bookmarks().whereEqualTo("userId", uid).limit(200).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.getString("postId") }
        }

    suspend fun bookmarkedPosts(uid: String): List<Post> = runCatching {
        val ids = bookmarks().whereEqualTo("userId", uid).limit(200).get().await()
            .documents
            .mapNotNull { it.getString("postId") }
        if (ids.isEmpty()) return@runCatching emptyList()
        firestore.collection(Collections.POSTS)
            .whereIn("postId", ids.take(10))
            .get()
            .await()
            .documents
            .mapNotNull { it.toPostOrNull() }
    }.getOrDefault(emptyList())

    suspend fun toggle(uid: String, postId: String): Result<Boolean> = runCatching {
        val docRef = bookmarks().document(bookmarkDocId(uid, postId))
        if (docRef.get().await().exists()) {
            docRef.delete().await()
            false
        } else {
            docRef.set(
                mapOf(
                    "bookmarkId" to docRef.id,
                    "userId" to uid,
                    "postId" to postId,
                    "createdAt" to Timestamp.now()
                )
            ).await()
            true
        }
    }
}
