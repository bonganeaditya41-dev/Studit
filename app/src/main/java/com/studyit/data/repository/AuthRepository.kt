package com.studyit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.studyit.core.Collections
import com.studyit.data.model.Rank
import com.studyit.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUserId: String? get() = auth.currentUser?.uid

    fun currentFirebaseUser(): FirebaseUser? = auth.currentUser

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun users() = firestore.collection(Collections.USERS)

    suspend fun isUsernameAvailable(username: String): Boolean {
        val lower = username.trim().lowercase()
        if (lower.isBlank()) return false
        val result = users().whereEqualTo("usernameLower", lower).limit(1).get().await()
        return result.isEmpty
    }

    private suspend fun uniqueUsername(base: String): String {
        var candidate = base.lowercase().replace(" ", "").replace(".", "")
        if (candidate.length < 3) candidate = "student"
        if (candidate.length > 16) candidate = candidate.take(16)
        var suffix = 0
        var attempt = candidate
        while (!isUsernameAvailable(attempt)) {
            suffix += 1
            attempt = candidate + suffix
            if (suffix > 200) break
        }
        return attempt
    }

    suspend fun signUp(
        fullName: String,
        username: String,
        email: String,
        password: String,
        academicClass: String
    ): Result<Unit> = runCatching {
        if (!isUsernameAvailable(username)) {
            throw IllegalStateException("That username is already taken")
        }
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = result.user ?: throw IllegalStateException("Could not create your account")
        val profile = User(
            uid = firebaseUser.uid,
            fullName = fullName.trim(),
            username = username.trim(),
            usernameLower = username.trim().lowercase(),
            email = email.trim(),
            academicClass = academicClass,
            rank = Rank.BEGINNER.label,
            totalPoints = 0L,
            createdAt = Timestamp.now()
        )
        users().document(firebaseUser.uid).set(profile).await()
        runCatching { firebaseUser.sendEmailVerification().await() }
        Unit
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    /** Returns true when a fresh profile had to be created (Google first sign-in). */
    suspend fun signInWithGoogleToken(idToken: String): Result<Boolean> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw IllegalStateException("Google sign-in failed")
        val docRef = users().document(firebaseUser.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            false
        } else {
            val base = (firebaseUser.displayName ?: "student")
                .lowercase()
                .replace(" ", "")
            val username = uniqueUsername(base)
            docRef.set(
                User(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Student",
                    username = username,
                    usernameLower = username.lowercase(),
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    academicClass = "Other",
                    createdAt = Timestamp.now()
                )
            ).await()
            true
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        user.sendEmailVerification().await()
        Unit
    }

    suspend fun reloadUser(): FirebaseUser? {
        val user = auth.currentUser ?: return null
        runCatching { user.reload().await() }
        return auth.currentUser
    }

    fun signOut() = auth.signOut()
}
