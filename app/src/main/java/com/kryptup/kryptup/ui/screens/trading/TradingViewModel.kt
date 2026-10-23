package com.kryptup.kryptup.ui.screens.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.data.repository.MarketRepository
import com.kryptup.kryptup.data.repository.TransactionRepository
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    val pinSecurityManager: PinSecurityManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prices = _prices.asStateFlow()

    private val _chartData = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val chartData = _chartData.asStateFlow()

    init {
        fetchPrices()
        fetchChartData("ethereum")
    }

    fun fetchPrices() {
        viewModelScope.launch {
            val response = marketRepository.getPrices("ethereum,tether,bitcoin,solana,cardano,dogecoin,matic-network,ripple")
            val priceMap = mutableMapOf<String, Double>()
            response.forEach { (id, data) ->
                data["usd"]?.let { priceMap[id] = it }
            }
            _prices.value = priceMap
        }
    }

    fun fetchChartData(coinId: String) {
        viewModelScope.launch {
            val response = marketRepository.getMarketChart(coinId, "1")
            response?.prices?.let { data ->
                val points = data.mapIndexed { index, list ->
                    Pair(index.toFloat(), list[1].toFloat())
                }
                _chartData.value = points
            }
        }
    }

    fun executeOrder(type: String, symbol: String, amountStr: String, price: Double) {
        viewModelScope.launch {
            val amount = amountStr.toDoubleOrNull() ?: 1.0
            val totalUsd = amount * price
            val isBuy = type.equals("Buy", ignoreCase = true)
            
            transactionRepository.recordTransaction(
                TransactionRecord(
                    type = if (isBuy) TransactionType.SPOT_BUY else TransactionType.SPOT_SELL,
                    title = "$type $amountStr $symbol",
                    description = "Spot Trade @ $${String.format("%.2f", price)}/unit",
                    amountDisplay = if (isBuy) "-$${String.format("%.2f", totalUsd)} USD" else "+$${String.format("%.2f", totalUsd)} USD",
                    amountValue = totalUsd,
                    isDebit = isBuy,
                    category = "Trading",
                    referenceId = "TRADE-" + UUID.randomUUID().toString().take(6).uppercase()
                )
            )
        }
    }
}
