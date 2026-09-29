package com.kryptup.kryptup.data.wallet

import android.content.Context
import android.content.SharedPreferences
import com.kryptup.kryptup.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip39Wallet
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.WalletUtils
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val sharedPreferences: SharedPreferences
) {
    private val walletDir = File(context.filesDir, "wallets")
    private val MNEMONIC_KEY = "encrypted_mnemonic"

    init {
        if (!walletDir.exists()) {
            walletDir.mkdirs()
        }
    }

    fun saveMnemonic(mnemonic: String) {
        sharedPreferences.edit().putString(MNEMONIC_KEY, mnemonic).apply()
    }

    fun getMnemonic(): String? {
        return sharedPreferences.getString(MNEMONIC_KEY, null)
    }

    fun hasWallet(): Boolean {
        return getMnemonic() != null
    }

    fun clearWallet() {
        sharedPreferences.edit().remove(MNEMONIC_KEY).apply()
        walletDir.listFiles()?.forEach { it.delete() }
    }

    fun generateMnemonic(): String {
        val initialEntropy = ByteArray(16)
        SecureRandom().nextBytes(initialEntropy)
        return MnemonicUtils.generateMnemonic(initialEntropy)
    }

    fun createWallet(mnemonic: String, pass: String): String {
        val seed = MnemonicUtils.generateSeed(mnemonic, pass)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        
        // BIP-44 Ethereum derivation path: m/44'/60'/0'/0/0
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        val credentials = Credentials.create(derivedKeyPair)
        
        // Save as V3 wallet file for persistence (optional if mnemonic is saved securely)
        val walletFileName = WalletUtils.generateWalletFile(pass, derivedKeyPair, walletDir, false)
        return credentials.address
    }

    fun importWallet(mnemonic: String, pass: String): String {
        // Importing is essentially deriving from the mnemonic
        return createWallet(mnemonic, pass)
    }

    fun getCredentialsFromMnemonic(mnemonic: String, pass: String): Credentials {
        val seed = MnemonicUtils.generateSeed(mnemonic, pass)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        return Credentials.create(derivedKeyPair)
    }
}
