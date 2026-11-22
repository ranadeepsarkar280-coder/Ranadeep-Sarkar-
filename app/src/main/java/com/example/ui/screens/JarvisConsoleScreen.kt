package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiMessage
import com.example.assistant.BengaliFormatter
import com.example.assistant.VoiceState
import com.example.ui.JarvisUiState
import com.example.ui.components.JarvisCoreOrb
import com.example.ui.theme.ArcCyanSoft
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.SilverMuted
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoidBlack

private val QuickBengaliCommands = listOf(
    "এখন কয়টা বাজে?",
    "আজ কী বার?",
    "ফোনের অবস্থা কী?",
    "ব্যাটারি কত?",
    "টর্চ জ্বালাও",
    "ইউটিউব খোলো",
    "সেটিংস খোলো",
    "ওয়াইফাই সেটিংস",
)

@Composable
fun JarvisConsoleScreen(
    uiState: JarvisUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleFlashlight: () -> Unit,
    onSubmitCommand: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
    onClearConversation: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var textInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = VoidBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VoidBlack),
        ) {
            // Subtle HUD backdrop artwork
            Image(
                painter = painterResource(id = R.drawable.jarvis_hud_backdrop_1795334801405),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.38f),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                val isWideScreen = maxWidth >= 720.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    OwnerIdentityHeader(
                        onClearConversation = onClearConversation,
                        onNavigateToSettings = onNavigateToSettings,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.46f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OrbAndTelemetrySection(
                                    uiState = uiState,
                                    onStartListening = onStartListening,
                                    onStopListening = onStopListening,
                                    onToggleBackgroundService = onToggleBackgroundService,
                                    onToggleFlashlight = onToggleFlashlight,
                                    onRequestMicPermission = onRequestMicPermission,
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.54f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ConversationSection(
                                    uiState = uiState,
                                    textInput = textInput,
                                    onTextInputChange = { textInput = it },
                                    onSend = {
                                        if (textInput.isNotBlank()) {
                                            onSubmitCommand(textInput)
                                            textInput = ""
                                        }
                                    },
                                    onQuickCommand = onSubmitCommand,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .align(Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OrbAndTelemetrySection(
                                uiState = uiState,
                                onStartListening = onStartListening,
                                onStopListening = onStopListening,
                                onToggleBackgroundService = onToggleBackgroundService,
                                onToggleFlashlight = onToggleFlashlight,
                                onRequestMicPermission = onRequestMicPermission,
                            )

                            ConversationSection(
                                uiState = uiState,
                                textInput = textInput,
                                onTextInputChange = { textInput = it },
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        onSubmitCommand(textInput)
                                        textInput = ""
                                    }
                                },
                                onQuickCommand = onSubmitCommand,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerIdentityHeader(
    onClearConversation: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        HudSurface.copy(alpha = 0.9f),
                        HudSurfaceElevated.copy(alpha = 0.75f),
                    )
                )
            )
            .border(1.dp, HudBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.owner_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MetallicSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.assistant_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = SilverMuted,
                )
                Text(
                    text = stringResource(id = R.string.system_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = ArcCyanSoft,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClearConversation,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("clear_chat_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear conversation",
                    tint = SilverMuted,
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("open_settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(id = R.string.cd_settings_button),
                    tint = ElectricBlue,
                )
            }
        }
    }
}

@Composable
private fun OrbAndTelemetrySection(
    uiState: JarvisUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleFlashlight: () -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    JarvisCoreOrb(
        voiceState = uiState.voiceState,
        micRmsLevel = uiState.micRmsLevel,
        isProcessing = uiState.isProcessingCommand,
        onToggleListen = {
            if (uiState.voiceState == VoiceState.IDLE) onStartListening() else onStopListening()
        },
    )

    if (uiState.partialTranscript.isNotBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ElectricBlue.copy(alpha = 0.12f))
                .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "শুনছি: \"${uiState.partialTranscript}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = ArcCyanSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    // Start / Stop Listening Primary Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = {
                if (uiState.hasMicPermission) onStartListening() else onRequestMicPermission()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = VoidBlack,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("start_listening_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.hasMicPermission) {
                    stringResource(id = R.string.action_start_listening)
                } else {
                    "মাইক্রোফোন অনুমতি"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        OutlinedButton(
            onClick = onStopListening,
            border = BorderStroke(
                1.dp,
                if (uiState.voiceState != VoiceState.IDLE) StatusDanger else HudBorder,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (uiState.voiceState != VoiceState.IDLE) StatusDanger else MetallicSilver,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("stop_listening_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.action_stop_listening),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    // Status & Background Service + Hardware Telemetry Card
    Card(
        colors = CardDefaults.cardColors(containerColor = HudSurface.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, HudBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusIndicatorPill(
                    icon = if (uiState.hasMicPermission) Icons.Filled.Mic else Icons.Filled.MicOff,
                    label = when {
                        !uiState.hasMicPermission -> stringResource(id = R.string.status_mic_permission_needed)
                        uiState.voiceState == VoiceState.ACTIVE_COMMAND_LISTENING -> "কমান্ড শুনছে"
                        uiState.voiceState == VoiceState.WAKE_WORD_LISTENING -> "ওয়েক-ওয়ার্ড শুনছে"
                        else -> stringResource(id = R.string.status_mic_ready)
                    },
                    dotColor = when {
                        !uiState.hasMicPermission -> StatusWarning
                        uiState.voiceState != VoiceState.IDLE -> StatusOnline
                        else -> ElectricBlue
                    },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (uiState.isServiceRunning) "ব্যাকগ্রাউন্ড চালু" else "ব্যাকগ্রাউন্ড বন্ধ",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isServiceRunning) StatusOnline else SilverMuted,
                    )
                    Switch(
                        checked = uiState.isServiceRunning,
                        onCheckedChange = onToggleBackgroundService,
                        modifier = Modifier.testTag("background_service_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoidBlack,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = SilverMuted,
                            uncheckedTrackColor = DeepSpace,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TelemetryChip(
                    icon = if (uiState.telemetry.isCharging) {
                        Icons.Filled.BatteryChargingFull
                    } else {
                        Icons.Filled.BatteryStd
                    },
                    label = "${BengaliFormatter.toBengaliDigits(uiState.telemetry.batteryPercent)}%",
                    accent = if (uiState.telemetry.batteryPercent <= 20) StatusWarning else StatusOnline,
                )

                TelemetryChip(
                    icon = Icons.Outlined.VolumeUp,
                    label = "ভলিউম ${BengaliFormatter.toBengaliDigits(uiState.telemetry.volumePercent)}%",
                    accent = ElectricBlue,
                )

                TelemetryChip(
                    icon = Icons.Filled.Wifi,
                    label = uiState.telemetry.networkLabelBn,
                    accent = ArcCyanSoft,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (uiState.telemetry.isTorchOn) {
                                ElectricBlue.copy(alpha = 0.22f)
                            } else {
                                DeepSpace
                            }
                        )
                        .border(
                            1.dp,
                            if (uiState.telemetry.isTorchOn) ElectricBlue else HudBorder,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable(onClick = onToggleFlashlight)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("torch_quick_toggle"),
                ) {
                    Icon(
                        imageVector = if (uiState.telemetry.isTorchOn) {
                            Icons.Filled.FlashlightOn
                        } else {
                            Icons.Filled.FlashlightOff
                        },
                        contentDescription = "Toggle flashlight",
                        tint = if (uiState.telemetry.isTorchOn) ElectricBlue else SilverMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.telemetry.isTorchOn) "টর্চ চালু" else "টর্চ বন্ধ",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.telemetry.isTorchOn) ArcCyanSoft else SilverMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    dotColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MetallicSilver,
        )
    }
}

@Composable
private fun TelemetryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DeepSpace)
            .border(1.dp, HudBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MetallicSilver,
        )
    }
}

@Composable
private fun ConversationSection(
    uiState: JarvisUiState,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Honest in-app Firebase AI notice when AI is in Placeholder mode
        if (!uiState.isCloudAiConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StatusWarning.copy(alpha = 0.10f))
                    .border(1.dp, StatusWarning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("firebase_ai_notice_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(id = R.string.ai_placeholder_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicSilver.copy(alpha = 0.9f),
                )
            }
        }

        // Quick Bengali Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickBengaliCommands.forEach { cmd ->
                AssistChip(
                    onClick = { onQuickCommand(cmd) },
                    label = {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = HudSurface,
                        labelColor = ArcCyanSoft,
                    ),
                    border = BorderStroke(1.dp, HudBorder),
                )
            }
        }

        // AI Conversation Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = HudSurface.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, HudBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("conversation_panel"),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ConversationBubble(message = msg)
                }
            }
        }

        // Text Input Command Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.input_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = HudBorder,
                    focusedTextColor = MetallicSilver,
                    unfocusedTextColor = MetallicSilver,
                    cursorColor = ElectricBlue,
                    focusedContainerColor = HudSurface,
                    unfocusedContainerColor = HudSurface,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
            )

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricBlue)
                    .testTag("send_command_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(id = R.string.cd_send_message),
                    tint = VoidBlack,
                )
            }
        }
    }
}

@Composable
private fun ConversationBubble(message: AiMessage) {
    val isBoss = message.sender == AiMessage.Sender.BOSS
    val isSystem = message.sender == AiMessage.Sender.SYSTEM

    val alignment = when {
        isBoss -> Alignment.End
        else -> Alignment.Start
    }
    val bubbleColor = when {
        isBoss -> ElectricBlue.copy(alpha = 0.18f)
        isSystem -> StatusWarning.copy(alpha = 0.14f)
        else -> HudSurfaceElevated
    }
    val borderColor = when {
        isBoss -> ElectricBlue.copy(alpha = 0.5f)
        isSystem -> StatusWarning.copy(alpha = 0.45f)
        else -> HudBorder
    }
    val senderLabel = when (message.sender) {
        AiMessage.Sender.BOSS -> "BOSS (Ranadeep)"
        AiMessage.Sender.JARVIS -> if (message.isPlaceholderResponse) "JARVIS • Placeholder" else "JARVIS"
        AiMessage.Sender.SYSTEM -> "SYSTEM"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = senderLabel,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isBoss -> ArcCyanSoft
                    isSystem -> StatusWarning
                    else -> ElectricBlue
                },
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MetallicSilver,
            )
        }
    }
}
