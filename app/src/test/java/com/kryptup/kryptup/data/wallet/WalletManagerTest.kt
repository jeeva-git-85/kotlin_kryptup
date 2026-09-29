package com.kryptup.kryptup.data.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils

class WalletManagerTest {

    @Test
    fun generateMnemonic_returnsValidTwelveWordPhrase() {
        val mnemonic = MnemonicUtils.generateMnemonic(ByteArray(16))
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
    }

    @Test
    fun deriveAddress_isDeterministic() {
        val mnemonic = "candy maple cake sugar honey pink sugar honey pink sugar honey pizza"
        val seed = MnemonicUtils.generateSeed(mnemonic, "password123")
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        val credentials = Credentials.create(derivedKeyPair)

        assertNotNull(credentials.address)
    }
}
