package com.kryptup.kryptup.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.model.Token
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.data.model.WalletTransaction
import com.kryptup.kryptup.data.repository.EthereumRepository
import com.kryptup.kryptup.data.repository.TransactionRepository
import com.kryptup.kryptup.data.wallet.WalletManager
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletManager: WalletManager,
    private val ethereumRepository: EthereumRepository,
    val pinSecurityManager: PinSecurityManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _mnemonic = MutableStateFlow("")
    val mnemonic: StateFlow<String> = _mnemonic

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _tokens = MutableStateFlow<List<Token>>(emptyList())
    val tokens: StateFlow<List<Token>> = _tokens

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    private val _totalBalanceUsd = MutableStateFlow(BigDecimal.ZERO)
    val totalBalanceUsd: StateFlow<BigDecimal> = _totalBalanceUsd

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _transactionStatus = MutableStateFlow<String?>(null)
    val transactionStatus = _transactionStatus.asStateFlow()

    init {
        val savedMnemonic = walletManager.getMnemonic()
        if (savedMnemonic != null) {
            _mnemonic.value = savedMnemonic
            createWallet("password123")
        }
    }

    fun generateMnemonic() {
        _mnemonic.value = walletManager.generateMnemonic()
    }

    fun createWallet(pass: String) {
        if (_mnemonic.value.isNotEmpty()) {
            walletManager.saveMnemonic(_mnemonic.value)
            _address.value = walletManager.createWallet(_mnemonic.value, pass)
            refreshBalances()
        }
    }

    fun refreshBalances() {
        val currentAddress = _address.value
        if (currentAddress.isNotEmpty()) {
            viewModelScope.launch {
                val ethBalance = ethereumRepository.getEthBalance(currentAddress)
                val usdtBalance = ethereumRepository.getErc20Balance(
                    "0x7169D3881550a327F110196142ccA57422789644", // Sepolia USDT
                    currentAddress,
                    6
                )
                
                _tokens.value = listOf(
                    Token("ETH", "Ethereum", null, ethBalance),
                    Token("USDT", "Tether", "0x7169D3881550a327F110196142ccA57422789644", usdtBalance, 6)
                )

                _transactions.value = ethereumRepository.getTransactionHistory(currentAddress)
                
                val ethPrice = BigDecimal("2500")
                val usdtPrice = BigDecimal("1")
                _totalBalanceUsd.value = ethBalance.multiply(ethPrice).add(usdtBalance.multiply(usdtPrice))
            }
        }
    }

    fun reset() {
        walletManager.clearWallet()
        _mnemonic.value = ""
        _address.value = ""
        _tokens.value = emptyList()
    }

    fun sendTransaction(toAddress: String, amountStr: String, symbol: String) {
        val amount = amountStr.toBigDecimalOrNull() ?: return
        val currentMnemonic = _mnemonic.value
        if (currentMnemonic.isEmpty()) return

        viewModelScope.launch {
            _isSending.value = true
            _transactionStatus.value = "Processing..."
            try {
                val credentials = walletManager.getCredentialsFromMnemonic(currentMnemonic, "password123")
                val hash = if (symbol == "ETH") {
                    ethereumRepository.sendEth(credentials, toAddress, amount)
                } else {
                    val token = _tokens.value.find { it.symbol == symbol }
                    ethereumRepository.sendErc20(
                        credentials,
                        token?.address ?: "",
                        toAddress,
                        amount,
                        token?.decimals ?: 18
                    )
                }
                _transactionStatus.value = "Success! Hash: $hash"

                // Log in TransactionRepository
                transactionRepository.recordTransaction(
                    TransactionRecord(
                        type = TransactionType.CRYPTO_SEND,
                        title = "Sent $amountStr $symbol",
                        description = "Transfer to ${toAddress.take(8)}...",
                        amountDisplay = "-$amountStr $symbol",
                        amountValue = amount.toDouble(),
                        isDebit = true,
                        category = "Transfer",
                        referenceId = hash.take(12)
                    )
                )

                refreshBalances()
            } catch (e: Exception) {
                _transactionStatus.value = "Error: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }
}
