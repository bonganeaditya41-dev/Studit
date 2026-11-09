package com.studyit.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadImage(uri: Uri, folder: String, ownerUid: String): Result<String> =
        runCatching {
            val path = folder + "/" + ownerUid + "/" + System.currentTimeMillis() + ".jpg"
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

    suspend fun uploadPostImage(uri: Uri, uid: String): Result<String> =
        uploadImage(uri, "posts", uid)

    suspend fun uploadProfileImage(uri: Uri, uid: String): Result<String> =
        uploadImage(uri, "profiles", uid)

    suspend fun uploadCommunityIcon(uri: Uri, uid: String): Result<String> =
        uploadImage(uri, "communities", uid)
}
