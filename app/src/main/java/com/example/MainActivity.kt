package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.JarvisViewModel
import com.example.ui.components.SecurityConfirmationDialog
import com.example.ui.screens.JarvisConsoleScreen
import com.example.ui.screens.SettingsPermissionsScreen
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.VoidBlack

const val ROUTE_JARVIS_CONSOLE = "jarvis_console"
const val ROUTE_SETTINGS_PERMISSIONS = "settings_permissions"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(VoidBlack.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(VoidBlack.toArgb()),
        )

        setContent {
            JarvisTheme {
                val viewModel: JarvisViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissionsAndTelemetry()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    viewModel.refreshPermissionsAndTelemetry()
                    if (granted) {
                        viewModel.startListening()
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) {
                    viewModel.refreshPermissionsAndTelemetry()
                }

                uiState.pendingConfirmation?.let { pending ->
                    SecurityConfirmationDialog(
                        confirmation = pending,
                        onApprove = { viewModel.resolveSecurityConfirmation(approved = true) },
                        onDeny = { viewModel.resolveSecurityConfirmation(approved = false) },
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_JARVIS_CONSOLE,
                ) {
                    composable(ROUTE_JARVIS_CONSOLE) {
                        JarvisConsoleScreen(
                            uiState = uiState,
                            onStartListening = { viewModel.startListening() },
                            onStopListening = { viewModel.stopListening() },
                            onToggleBackgroundService = { enable ->
                                if (enable && !uiState.hasMicPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.toggleBackgroundService(enable)
                                }
                            },
                            onToggleFlashlight = { viewModel.toggleFlashlightDirect() },
                            onSubmitCommand = { cmd -> viewModel.submitTextCommand(cmd) },
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onClearConversation = { viewModel.clearConversation() },
                            onNavigateToSettings = {
                                navController.navigate(ROUTE_SETTINGS_PERMISSIONS) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    composable(ROUTE_SETTINGS_PERMISSIONS) {
                        SettingsPermissionsScreen(
                            uiState = uiState,
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            },
                            onOpenBatterySettings = { viewModel.openBatteryOptimizationSettings() },
                            onOpenAppSettings = { viewModel.openAppSystemSettings() },
                            onWakeWordChanged = { viewModel.setWakeWordEnabled(it) },
                            onVoiceResponseChanged = { viewModel.setVoiceResponseEnabled(it) },
                            onSpeechRateChanged = { viewModel.setTtsSpeechRate(it) },
                            onSecurityConfirmationChanged = {
                                viewModel.setSecurityConfirmationRequired(it)
                            },
                            onAutoNotifyOnBootChanged = { viewModel.setAutoNotifyOnBoot(it) },
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
