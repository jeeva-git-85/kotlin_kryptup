package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersRef = firestore.collection("users")

    suspend fun addPoints(points: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.document(uid).update("points", FieldValue.increment(points.toLong())).await()
        } catch (e: Exception) {
            // Handle offline or error case
        }
    }

    suspend fun claimDailyLoginReward(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = usersRef.document(uid).get().await()
            val lastClaim = doc.getLong("lastDailyClaim") ?: 0L
            val currentTime = System.currentTimeMillis()

            // Check if 24 hours have passed
            if (currentTime - lastClaim >= 86400000) {
                usersRef.document(uid).update(
                    mapOf(
                        "points" to FieldValue.increment(50),
                        "lastDailyClaim" to currentTime
                    )
                ).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
