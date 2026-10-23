package com.kryptup.kryptup.security

import android.content.SharedPreferences
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

enum class PinVerificationResult {
    SUCCESS,
    INVALID_PIN,
    LOCKED_OUT
}

@Singleton
class PinSecurityManager @Inject constructor(
    private val prefs: SharedPreferences,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    private fun getUid(): String {
        return auth.currentUser?.uid ?: "default_user"
    }

    private fun getPinKey() = "user_tx_pin_hash_${getUid()}"
    private fun getSaltKey() = "user_tx_pin_salt_${getUid()}"
    private fun getBiometricKey() = "user_tx_biometric_enabled_${getUid()}"
    private fun getFailedAttemptsKey() = "user_tx_pin_failed_attempts_${getUid()}"
    private fun getLockoutTimeKey() = "user_tx_pin_lockout_time_${getUid()}"

    fun isPinSet(): Boolean {
        val hash = prefs.getString(getPinKey(), null)
        return !hash.isNullOrEmpty()
    }

    private fun getOrCreateSalt(): String {
        val saltKey = getSaltKey()
        var salt = prefs.getString(saltKey, null)
        if (salt == null) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            prefs.edit().putString(saltKey, salt).apply()
        }
        return salt
    }

    fun setPin(pin: String) {
        val salt = getOrCreateSalt()
        val hashedPin = hashPin(pin, salt)
        prefs.edit()
            .putString(getPinKey(), hashedPin)
            .putInt(getFailedAttemptsKey(), 0)
            .putLong(getLockoutTimeKey(), 0L)
            .apply()
    }

    fun isLockedOut(): Boolean {
        val lockoutTime = prefs.getLong(getLockoutTimeKey(), 0L)
        if (lockoutTime == 0L) return false
        val now = System.currentTimeMillis()
        if (now - lockoutTime < LOCKOUT_DURATION_MS) {
            return true
        } else {
            // Lockout expired - reset
            prefs.edit()
                .putInt(getFailedAttemptsKey(), 0)
                .putLong(getLockoutTimeKey(), 0L)
                .apply()
            return false
        }
    }

    fun getRemainingLockoutTimeSeconds(): Long {
        val lockoutTime = prefs.getLong(getLockoutTimeKey(), 0L)
        if (lockoutTime == 0L) return 0L
        val remainingMs = LOCKOUT_DURATION_MS - (System.currentTimeMillis() - lockoutTime)
        return if (remainingMs > 0) remainingMs / 1000 else 0L
    }

    fun verifyPinResult(enteredPin: String): PinVerificationResult {
        if (isLockedOut()) {
            return PinVerificationResult.LOCKED_OUT
        }

        val storedHash = prefs.getString(getPinKey(), null) ?: return PinVerificationResult.INVALID_PIN
        val salt = getOrCreateSalt()
        val computedHash = hashPin(enteredPin, salt)

        if (storedHash == computedHash) {
            // Reset failed attempts on success
            prefs.edit()
                .putInt(getFailedAttemptsKey(), 0)
                .putLong(getLockoutTimeKey(), 0L)
                .apply()
            return PinVerificationResult.SUCCESS
        } else {
            val failedAttempts = prefs.getInt(getFailedAttemptsKey(), 0) + 1
            val editor = prefs.edit().putInt(getFailedAttemptsKey(), failedAttempts)
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                editor.putLong(getLockoutTimeKey(), System.currentTimeMillis())
            }
            editor.apply()

            return if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                PinVerificationResult.LOCKED_OUT
            } else {
                PinVerificationResult.INVALID_PIN
            }
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        return verifyPinResult(enteredPin) == PinVerificationResult.SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(getBiometricKey(), true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(getBiometricKey(), enabled).apply()
    }

    fun clearPin() {
        prefs.edit()
            .remove(getPinKey())
            .remove(getSaltKey())
            .remove(getBiometricKey())
            .remove(getFailedAttemptsKey())
            .remove(getLockoutTimeKey())
            .apply()
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val bytes = md.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
