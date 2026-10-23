package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val transactionRepository: TransactionRepository
) {
    
    suspend fun verifyCard(cardNumber: String, expiry: String, cvv: String): Boolean {
        delay(1500)
        return cardNumber.length == 16
    }

    suspend fun verifyOtp(otp: String): Boolean = withContext(Dispatchers.IO) {
        delay(1000)
        if (otp == "123456") {
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                try {
                    val initialDeposit = 10000.00
                    firestore.collection("users").document(currentUid)
                        .update("fiatBalanceInr", FieldValue.increment(initialDeposit))
                        .await()

                    transactionRepository.recordTransaction(
                        TransactionRecord(
                            type = TransactionType.BANK_DEPOSIT,
                            title = "Bank Account Linked & Initial Deposit",
                            description = "Initial simulated bank balance added",
                            amountDisplay = "+₹10,000.00 INR",
                            amountValue = initialDeposit,
                            isDebit = false,
                            category = "Banking",
                            referenceId = "BANK-LINK-" + UUID.randomUUID().toString().take(6).uppercase()
                        )
                    )
                } catch (e: Exception) {
                    // Fallback
                }
            }
            return@withContext true
        }
        return@withContext false
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

            // Record transaction in TransactionRepository
            val refId = "UPI-TX-" + UUID.randomUUID().toString().take(8).uppercase()
            transactionRepository.recordTransaction(
                TransactionRecord(
                    type = TransactionType.UPI_SEND,
                    title = "UPI Payment to $recipientUpiId",
                    description = "P2P Instant UPI Transfer",
                    amountDisplay = "-₹${String.format("%.2f", amountInr)} INR",
                    amountValue = amountInr,
                    isDebit = true,
                    category = "Transfer",
                    referenceId = refId
                )
            )

            Result.success("Sent ₹${String.format("%.2f", amountInr)} to $recipientUpiId")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
