package com.kryptup.kryptup.ui.screens.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val marketRepository: MarketRepository
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
            val response = marketRepository.getPrices("ethereum,tether,bitcoin")
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
}
