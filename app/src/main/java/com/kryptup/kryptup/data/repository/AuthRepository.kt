package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kryptup.kryptup.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val currentUid = auth.currentUser?.uid ?: ""
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .filter { it.uid.isNotEmpty() && it.uid != currentUid }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun parseAuthError(e: Exception): Exception {
        val msg = e.localizedMessage ?: ""
        return if (msg.contains("unexpected end of stream") || msg.contains("IOException") || msg.contains("internal error")) {
            Exception("Network connection interrupted. Please try again.")
        } else {
            e
        }
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Login failed: User is null"))
        } catch (e: Exception) {
            Result.failure(parseAuthError(e))
        }
    }

    suspend fun getCurrentUserProfile(): User? = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed: User is null")
            
            val sanitized = name.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
            val uidSuffix = firebaseUser.uid.take(4).lowercase()
            val upiId = "$sanitized$uidSuffix@kryptup"

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = name,
                upiId = upiId,
                fiatBalanceInr = 0.00
            )
            firestore.collection("users").document(firebaseUser.uid).set(user).await()
            
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(parseAuthError(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(Exception("Please enter a valid email address (e.g. user@gmail.com)"))
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Retry once on network glitch
            try {
                auth.sendPasswordResetEmail(cleanEmail).await()
                Result.success(Unit)
            } catch (retryEx: Exception) {
                Result.failure(parseAuthError(retryEx))
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun firebaseSignInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed: User is null")
            
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                val displayName = firebaseUser.displayName ?: "User"
                val sanitized = displayName.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
                val uidSuffix = firebaseUser.uid.take(4).lowercase()
                val upiId = "$sanitized$uidSuffix@kryptup"

                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName,
                    upiId = upiId,
                    fiatBalanceInr = 0.00
                )
                firestore.collection("users").document(firebaseUser.uid).set(user).await()
            }
            
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
