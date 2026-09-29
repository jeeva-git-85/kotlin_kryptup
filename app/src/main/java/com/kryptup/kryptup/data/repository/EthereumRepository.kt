package com.kryptup.kryptup.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.methods.request.Transaction as Web3jTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import org.web3j.crypto.Credentials
import org.web3j.tx.ChainId
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.tx.gas.StaticGasProvider
import com.kryptup.kryptup.data.model.WalletTransaction
import com.kryptup.kryptup.data.model.TransactionStatus

@Singleton
class EthereumRepository @Inject constructor(
    private val web3j: Web3j
) {
    suspend fun getTransactionHistory(address: String): List<WalletTransaction> = withContext(Dispatchers.IO) {
        // Mock data
        listOf(
            WalletTransaction(
                hash = "0x123...abc",
                from = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
                to = address,
                value = BigDecimal("0.5"),
                symbol = "ETH",
                timestamp = System.currentTimeMillis() - 86400000,
                isIncoming = true
            ),
            WalletTransaction(
                hash = "0x456...def",
                from = address,
                to = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
                value = BigDecimal("100"),
                symbol = "USDT",
                timestamp = System.currentTimeMillis() - 3600000,
                isIncoming = false
            )
        )
    }

    suspend fun getEthBalance(address: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()
            val balanceWei = balanceResponse.balance
            Convert.fromWei(balanceWei.toString(), Convert.Unit.ETHER)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun getErc20Balance(contractAddress: String, ownerAddress: String, decimals: Int): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "balanceOf",
                listOf(Address(ownerAddress)),
                listOf(object : TypeReference<Uint256>() {})
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Web3jTransaction.createEthCallTransaction(ownerAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()
            
            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val balance = results[0].value as BigInteger
                balance.toBigDecimal().divide(BigDecimal.TEN.pow(decimals))
            } else {
                BigDecimal.ZERO
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun sendEth(credentials: Credentials, toAddress: String, amountEth: BigDecimal): String = withContext(Dispatchers.IO) {
        val transactionReceipt = Transfer.sendFunds(
            web3j, credentials, toAddress,
            amountEth, Convert.Unit.ETHER
        ).send()
        transactionReceipt.transactionHash
    }

    suspend fun sendErc20(
        credentials: Credentials,
        contractAddress: String,
        toAddress: String,
        amount: BigDecimal,
        decimals: Int
    ): String = withContext(Dispatchers.IO) {
        val amountWei = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()
        
        val function = Function(
            "transfer",
            listOf(Address(toAddress), Uint256(amountWei)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val chainId: Long = 11155111 // Sepolia Chain ID
        val transactionManager = RawTransactionManager(web3j, credentials, chainId)
        
        val gasProvider = DefaultGasProvider()
        val ethSendTransaction = transactionManager.sendTransaction(
            gasProvider.getGasPrice("transfer"),
            gasProvider.getGasLimit("transfer"),
            contractAddress,
            encodedFunction,
            BigInteger.ZERO
        )

        if (ethSendTransaction.hasError()) {
            throw Exception(ethSendTransaction.error.message)
        }
        ethSendTransaction.transactionHash
    }
}
