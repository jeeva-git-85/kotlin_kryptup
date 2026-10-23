package com.kryptup.kryptup.security

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinSecurityManager @Inject constructor(
    private val prefs: SharedPreferences,
    private val auth: FirebaseAuth
) {
    private fun getPinKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_pin_hash_$uid"
    }

    private fun getBiometricKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_biometric_enabled_$uid"
    }

    fun isPinSet(): Boolean {
        val hash = prefs.getString(getPinKey(), null)
        return !hash.isNullOrEmpty()
    }

    fun setPin(pin: String) {
        val hashedPin = hashPin(pin)
        prefs.edit().putString(getPinKey(), hashedPin).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedHash = prefs.getString(getPinKey(), null) ?: return false
        return storedHash == hashPin(enteredPin)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(getBiometricKey(), true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(getBiometricKey(), enabled).apply()
    }

    fun clearPin() {
        prefs.edit().remove(getPinKey()).remove(getBiometricKey()).apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
