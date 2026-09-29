package com.kryptup.kryptup.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.security.BiometricPromptManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    initialAddress: String? = null,
    onBackClick: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    var toAddress by remember { mutableStateOf(initialAddress ?: "") }
    var amount by remember { mutableStateOf("") }
    var selectedToken by remember { mutableStateOf("ETH") }
    
    val tokens by viewModel.tokens.collectAsState()
    val isLoading by viewModel.isSending.collectAsState()
    val status by viewModel.transactionStatus.collectAsState()

    val context = LocalContext.current
    val biometricPromptManager = remember { 
        BiometricPromptManager(context as FragmentActivity) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Assets") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = toAddress,
                onValueChange = { toAddress = it },
                label = { Text("Recipient Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0x...") }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text("Select Token", style = MaterialTheme.typography.labelMedium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tokens.forEach { token ->
                    FilterChip(
                        selected = selectedToken == token.symbol,
                        onClick = { selectedToken = token.symbol },
                        label = { Text(token.symbol) }
                    )
                }
            }

            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    biometricPromptManager.showBiometricPrompt(
                        title = "Authorize Transaction",
                        subtitle = "Confirm sending $amount $selectedToken",
                        onSuccess = {
                            viewModel.sendTransaction(toAddress, amount, selectedToken)
                        },
                        onError = {
                            // Handle error - maybe show a snackbar or message
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = toAddress.isNotEmpty() && amount.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Send")
                }
            }
        }
    }
}
