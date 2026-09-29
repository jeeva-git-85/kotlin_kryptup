package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kryptup.kryptup.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    suspend fun verifyCard(cardNumber: String, expiry: String, cvv: String): Boolean {
        delay(1500) // Simulate network delay
        return cardNumber.length == 16
    }

    suspend fun verifyOtp(otp: String): Boolean {
        delay(1000)
        return otp == "123456" // Mock OTP
    }

    suspend fun sendUpiMoney(recipientUpiId: String, amountInr: Double): Result<String> = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("User not authenticated"))
        
        try {
            val senderDoc = firestore.collection("users").document(currentUid).get().await()
            val sender = senderDoc.toObject(User::class.java) ?: return@withContext Result.failure(Exception("User profile not found"))

            if (sender.fiatBalanceInr < amountInr) {
                return@withContext Result.failure(Exception("Insufficient balance. Available: ₹${String.format("%.2f", sender.fiatBalanceInr)}"))
            }

            val recipientQuery = firestore.collection("users")
                .whereEqualTo("upiId", recipientUpiId.lowercase().trim())
                .get().await()

            val recipientDoc = recipientQuery.documents.firstOrNull()
                ?: return@withContext Result.failure(Exception("UPI ID '$recipientUpiId' not found"))

            val recipientUid = recipientDoc.id

            if (recipientUid == currentUid) {
                return@withContext Result.failure(Exception("Cannot send money to your own UPI ID"))
            }

            firestore.runBatch { batch ->
                batch.update(firestore.collection("users").document(currentUid), "fiatBalanceInr", FieldValue.increment(-amountInr))
                batch.update(firestore.collection("users").document(recipientUid), "fiatBalanceInr", FieldValue.increment(amountInr))
            }.await()

            Result.success("Sent ₹${String.format("%.2f", amountInr)} to $recipientUpiId")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
