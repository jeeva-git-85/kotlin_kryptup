package com.kryptup.kryptup.ui.screens.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.BuildConfig
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.ui.theme.*
import com.kryptup.kryptup.voice.AgentAction
import com.kryptup.kryptup.voice.FinancialNlpEngine
import com.kryptup.kryptup.voice.GeminiLiveWebSocketManager
import com.kryptup.kryptup.voice.GeminiManager
import com.kryptup.kryptup.voice.TextToSpeechManager
import com.kryptup.kryptup.voice.VoiceAssistantManager
import com.kryptup.kryptup.voice.VoiceIntent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimeframeData(
    val changeText: String,
    val isPositive: Boolean,
    val points: List<Float>,
    val strokeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBankLinking: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToTrading: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userFirstName by viewModel.userFirstName.collectAsState()
    val fiatBalanceInr by viewModel.fiatBalanceInr.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val totalMonthlySpentUsd by viewModel.totalMonthlySpentUsd.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedTxFilter by remember { mutableStateOf("All") }
    var selectedTxDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var assistantResponse by remember { mutableStateOf("") }
    var isBalanceHidden by remember { mutableStateOf(false) }
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var selectedCurrency by remember { mutableStateOf("USD") }

    val balanceUsd = fiatBalanceInr / 83.50

    val ttsManager = remember { TextToSpeechManager(context) }
    val nlpEngine = remember { FinancialNlpEngine() }
    val geminiManager = remember { GeminiManager() }
    val coroutineScope = rememberCoroutineScope()

    val geminiLiveWs = remember {
        GeminiLiveWebSocketManager(
            apiKey = BuildConfig.GEMINI_API_KEY,
            onMessageReceived = { responseText ->
                assistantResponse = responseText
                ttsManager.speak(responseText)
            },
            onError = { _ -> }
        ).apply { connect() }
    }

    val voiceManager = remember {
        VoiceAssistantManager(
            context = context,
            onResult = { text ->
                isListening = false
                val intent = nlpEngine.parseCommand(text)
                when (intent) {
                    is VoiceIntent.CheckBalance -> {
                        assistantResponse = "Your current portfolio balance is $${String.format("%.2f", balanceUsd)} USD."
                        ttsManager.speak(assistantResponse)
                    }
                    is VoiceIntent.CheckPrice -> {
                        assistantResponse = "The price of ${intent.symbol} is $2,500 USD."
                        ttsManager.speak(assistantResponse)
                    }
                    is VoiceIntent.SendCrypto -> {
                        assistantResponse = "Parsed transfer command: ${intent.amount} ${intent.symbol} to ${intent.recipient}."
                        ttsManager.speak(assistantResponse)
                        onNavigateToWallet()
                    }
                    is VoiceIntent.NavigateScreen -> {
                        val msg = "Opening ${intent.screen}..."
                        assistantResponse = msg
                        ttsManager.speak(msg)
                        when (intent.screen) {
                            "wallet" -> onNavigateToWallet()
                            "bank" -> onNavigateToBankLinking()
                            "trading" -> onNavigateToTrading()
                            "scanner" -> onNavigateToScanner()
                            "chat" -> onNavigateToChat()
                            "rewards" -> onNavigateToRewards()
                        }
                    }
                    is VoiceIntent.Unknown -> {
                        assistantResponse = "Agent processing..."
                        coroutineScope.launch {
                            val action = geminiManager.executeAgentRequest(text)
                            when (action) {
                                is AgentAction.TextResponse -> {
                                    assistantResponse = action.text
                                    ttsManager.speak(action.text)
                                }
                                is AgentAction.NavigateScreen -> {
                                    assistantResponse = action.message
                                    ttsManager.speak(action.message)
                                    when (action.screen.lowercase()) {
                                        "wallet", "vault" -> onNavigateToWallet()
                                        "bank", "linking" -> onNavigateToBankLinking()
                                        "trading", "trade" -> onNavigateToTrading()
                                        "scanner", "scan" -> onNavigateToScanner()
                                        "chat" -> onNavigateToChat()
                                        "rewards", "reward" -> onNavigateToRewards()
                                    }
                                }
                                is AgentAction.SendCrypto -> {
                                    assistantResponse = action.message
                                    ttsManager.speak(action.message)
                                    onNavigateToWallet()
                                }
                            }
                        }
                    }
                }
            },
            onError = { error ->
                isListening = false
                assistantResponse = "Voice AI: $error"
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            assistantResponse = "Listening..."
            isListening = true
            voiceManager.startListening()
        } else {
            assistantResponse = "Microphone permission is required for Voice AI."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
            voiceManager.destroy()
            geminiLiveWs.disconnect()
        }
    }

    val filteredTransactions = remember(transactions, selectedTxFilter) {
        if (selectedTxFilter == "All") {
            transactions
        } else {
            transactions.filter { it.category.equals(selectedTxFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = LightBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                        isListening = false
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            assistantResponse = "Listening..."
                            isListening = true
                            voiceManager.startListening()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (isListening) CoralRed else BrandCharcoal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. TopBar Profile & Notification Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToProfile() }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftAqua),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userFirstName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SoftAquaText
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WELCOME BACK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.2.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userFirstName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = LightSurfaceVariant,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.clickable {
                                showNotificationsSheet = true
                                viewModel.markNotificationsRead()
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LightSurfaceVariant,
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (unreadCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE03137),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Voice AI Banner
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD1FAE5),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voice AI Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                                Text(
                                    text = assistantResponse.ifEmpty { "\"Check BTC price\" or \"Send $50 USDT\"" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                    isListening = false
                                } else {
                                    assistantResponse = "Listening..."
                                    isListening = true
                                    voiceManager.startListening()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Talk", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Total Portfolio Net Worth
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TOTAL PORTFOLIO NET WORTH",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { isBalanceHidden = !isBalanceHidden },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Balance",
                                    tint = TextMuted
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3F4F6),
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.clickable {
                                    selectedCurrency = if (selectedCurrency == "USD") "INR" else "USD"
                                }
                            ) {
                                Text(
                                    text = if (selectedCurrency == "USD") "$ USD" else "₹ INR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreen,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val displayBalance = if (isBalanceHidden) {
                        "••••••••"
                    } else if (selectedCurrency == "USD") {
                        "$${String.format("%.2f", balanceUsd)}"
                    } else {
                        "₹${String.format("%.2f", fiatBalanceInr)}"
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayBalance,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCurrency,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val timeframeData = when (selectedTimeframe) {
                        "1D" -> TimeframeData("+$0.00 (+0.00%)", true, listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f), BrandCharcoal)
                        "1W" -> TimeframeData("+$0.00 (+0.00%)", true, listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f), EmeraldGreen)
                        "1M" -> TimeframeData("+$0.00 (+0.00%)", true, listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f), EmeraldGreen)
                        "1Y" -> TimeframeData("+$0.00 (+0.00%)", true, listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f), EmeraldGreen)
                        else -> TimeframeData("+$0.00 (+0.00%)", true, listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f), EmeraldGreen)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = timeframeData.changeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedTimeframe,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        val path = Path()
                        timeframeData.points.forEachIndexed { index, value ->
                            val x = (index.toFloat() / (timeframeData.points.size - 1)) * w
                            val y = h - (value * h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = timeframeData.strokeColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val lastX = w
                        val lastY = h - (timeframeData.points.last() * h)
                        drawCircle(
                            color = timeframeData.strokeColor,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1D", "1W", "1M", "1Y", "All").forEach { tf ->
                            val isActive = selectedTimeframe == tf
                            Surface(
                                shape = CircleShape,
                                color = if (isActive) Color(0xFFF2F4F7) else Color.Transparent,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { selectedTimeframe = tf }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tf,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isActive) TextPrimary else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButtonSpec(
                        icon = Icons.Default.AccountBalance,
                        label = "Add funds",
                        isPrimary = true,
                        onClick = onNavigateToBankLinking
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.QrCodeScanner,
                        label = "Pay",
                        onClick = onNavigateToScanner
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.Add,
                        label = "Buy",
                        onClick = onNavigateToTrading
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.NorthEast,
                        label = "Send",
                        onClick = onNavigateToWallet
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.MoreHoriz,
                        label = "More",
                        onClick = onNavigateToChat
                    )
                }
            }

            // 5. Complete Real-Time Spending Analytics & Transaction History Feed
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spending & Transaction Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Total Outflow: $${String.format("%.2f", totalMonthlySpentUsd)} USD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LightSurfaceVariant,
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Text(
                                text = "${transactions.size} Records",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf("All", "Transfer", "Trading", "Banking", "Rewards")
                        items(categories) { category ->
                            val isSelected = selectedTxFilter == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTxFilter = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCharcoal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredTransactions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LightSurface,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transaction records found for '$selectedTxFilter'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredTransactions.take(8).forEach { tx ->
                                TransactionRecordTile(
                                    record = tx,
                                    onClick = { selectedTxDetail = tx }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Market Watchlist Ticker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Market Watchlist",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "See all >",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToTrading() }
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sampleWatchlist) { item ->
                        WatchlistCardLight(item, selectedCurrency)
                    }
                }
            }

            // 7. Feature Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandCharcoal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GLOBAL CROSS-BORDER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MintGreen,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "See what's new in World Money",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Instant SEPA & UPI conversion straight into crypto vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToBankLinking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandCharcoal),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Explore now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF27272A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Super-App Hub Grid Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Super-App Hub",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Integrated DeFi, Fiat & Social Suite",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF2F4F7),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text(
                            text = "v2.4 Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Link Bank",
                            subtitle = "Fiat Cards & Instant OTP",
                            icon = Icons.Default.CreditCard,
                            accentColor = CyberCyan,
                            onClick = onNavigateToBankLinking
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Crypto Vault",
                            subtitle = "MPC Multi-Sig Security",
                            icon = Icons.Default.Shield,
                            accentColor = MintGreen,
                            onClick = onNavigateToWallet
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Spot Trading",
                            subtitle = "Live Orderbooks & Depth",
                            icon = Icons.Default.BarChart,
                            accentColor = ElectricViolet,
                            onClick = onNavigateToTrading
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "P2P Scanner",
                            subtitle = "Instant Barcode Pay",
                            icon = Icons.Default.QrCodeScanner,
                            accentColor = CyberCyan,
                            onClick = onNavigateToScanner
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Social Chat",
                            subtitle = "Pay Inside DM Threads",
                            icon = Icons.Default.Chat,
                            accentColor = MintGreen,
                            onClick = onNavigateToChat
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Rewards Hub",
                            subtitle = "Claim Daily APY Booster",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = GoldAmber,
                            onClick = onNavigateToRewards
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                containerColor = LightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showNotificationsSheet = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No message notifications yet.", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(notifications) { notif ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = LightSurfaceVariant,
                                    border = BorderStroke(1.dp, LightBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNotificationsSheet = false
                                            onNavigateToChat()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = null,
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "New Message from ${notif.senderName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = notif.messageText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transaction Detail Modal BottomSheet
        selectedTxDetail?.let { record ->
            ModalBottomSheet(
                onDismissRequest = { selectedTxDetail = null },
                containerColor = LightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { selectedTxDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = record.amountDisplay,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (record.isDebit) CoralRed else EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = record.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LightSurfaceVariant,
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Title", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Category", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Description", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.description, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timestamp", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reference ID", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        record.referenceId,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(record.referenceId)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionRecordTile(
    record: TransactionRecord,
    onClick: () -> Unit
) {
    val icon = when (record.type) {
        TransactionType.UPI_SEND, TransactionType.UPI_RECEIVE -> Icons.Default.AccountBalance
        TransactionType.CRYPTO_SEND, TransactionType.CRYPTO_RECEIVE -> Icons.Default.NorthEast
        TransactionType.SPOT_BUY, TransactionType.SPOT_SELL -> Icons.Default.BarChart
        TransactionType.BANK_DEPOSIT -> Icons.Default.CreditCard
        TransactionType.REWARD_CLAIM -> Icons.Default.EmojiEvents
    }

    val iconColor = when (record.category) {
        "Trading" -> ElectricViolet
        "Banking" -> CyberCyan
        "Rewards" -> GoldAmber
        else -> EmeraldGreen
    }

    val formattedTime = remember(record.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightSurface,
        border = BorderStroke(1.dp, LightBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "$formattedTime • ${record.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = record.amountDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (record.isDebit) TextPrimary else EmeraldGreen
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        text = record.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButtonSpec(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = if (isPrimary) BrandCharcoal else Color(0xFFF4F5F7),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPrimary) Color.White else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val priceUsd: String,
    val priceInr: String,
    val change: String,
    val isPositive: Boolean
)

val sampleWatchlist = listOf(
    WatchlistItem("BTC", "Bitcoin", "$94,250.00", "₹78,70,000", "+2.4%", true),
    WatchlistItem("ETH", "Ethereum", "$2,500.00", "₹2,08,750", "+1.8%", true),
    WatchlistItem("USDT", "Tether", "$1.00", "₹83.50", "0.0%", true),
    WatchlistItem("SOL", "Solana", "$185.50", "₹15,489", "-0.9%", false)
)

@Composable
fun WatchlistCardLight(item: WatchlistItem, currency: String = "USD") {
    val displayPrice = if (currency == "INR") item.priceInr else item.priceUsd

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, LightBorder),
        modifier = Modifier.width(135.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isPositive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = item.change,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (item.isPositive) EmeraldGreen else CoralRed,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayPrice,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SuperAppCardLight(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LightBorder),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }
        }
    }
}
