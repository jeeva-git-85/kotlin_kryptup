package com.kryptup.kryptup

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.kryptup.kryptup.security.BiometricPromptManager
import com.kryptup.kryptup.ui.navigation.SetupNavGraph
import com.kryptup.kryptup.ui.theme.KryptupTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent screen capture / screenshots on sensitive wallet & PIN screens
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        enableHighRefreshRate()
        
        val biometricPromptManager = BiometricPromptManager(this)

        setContent {
            KryptupTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    biometricPromptManager.showBiometricPrompt(
                        title = "KryptUp Secure Entry",
                        subtitle = "Authenticate to access your wallet",
                        onSuccess = { isAuthenticated = true },
                        onNotAvailable = { isAuthenticated = true },
                        onError = { _, _ ->
                            // Do NOT allow bypass on error or cancel; maintain authentication lock
                            isAuthenticated = false
                        }
                    )
                }

                if (isAuthenticated) {
                    val navController = rememberNavController()
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SetupNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentDisplay = display
            if (currentDisplay != null) {
                val supportedModes = currentDisplay.supportedModes
                // Find mode with highest refresh rate (e.g. 120Hz / 144Hz)
                val maxMode = supportedModes.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = maxMode.modeId
                    window.attributes = layoutParams
                }
            }
        }
    }
}
