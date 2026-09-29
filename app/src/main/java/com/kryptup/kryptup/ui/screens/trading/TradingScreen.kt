package com.kryptup.kryptup.ui.screens.trading

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    onBackClick: () -> Unit,
    viewModel: TradingViewModel = hiltViewModel()
) {
    val prices by viewModel.prices.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf("Buy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trading") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Market Data", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            prices.forEach { (coin, price) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = coin.replaceFirstChar { it.uppercase() })
                        Text(text = "$${String.format("%.2f", price)}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Price Chart (ETH/USD)", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            PriceChart(data = chartData)

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { 
                        orderType = "Buy"
                        showOrderDialog = true 
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Buy")
                }
                Button(
                    onClick = { 
                        orderType = "Sell"
                        showOrderDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sell")
                }
            }
        }
    }

    if (showOrderDialog) {
        OrderDialog(
            type = orderType,
            onDismiss = { showOrderDialog = false },
            onConfirm = { amount ->
                // TODO: Process order in VM
                showOrderDialog = false
            }
        )
    }
}

@Composable
fun OrderDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$type Ethereum") },
        text = {
            Column {
                Text("Enter the amount of ETH you want to ${type.lowercase()}.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (ETH)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PriceChart(data: List<Pair<Float, Float>>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                xAxis.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.textColor = AndroidColor.GRAY
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Price").apply {
                color = AndroidColor.BLUE
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
