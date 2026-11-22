package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.JarvisUiState
import com.example.ui.theme.ArcCyanSoft
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.SilverMuted
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoidBlack

@Composable
fun SettingsPermissionsScreen(
    uiState: JarvisUiState,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onWakeWordChanged: (Boolean) -> Unit,
    onVoiceResponseChanged: (Boolean) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onSecurityConfirmationChanged: (Boolean) -> Unit,
    onAutoNotifyOnBootChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
) {
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = VoidBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back_button),
                            tint = ElectricBlue,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.nav_settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MetallicSilver,
                        )
                        Text(
                            text = "RANADEEP SARKAR • JARVIS SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricBlue,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeading("অনুমতি ও নিরাপত্তা (Permissions)")

                    PermissionCard(
                        icon = Icons.Filled.Mic,
                        title = "মাইক্রোফোন অনুমতি (RECORD_AUDIO)",
                        explanation = "ওয়েক-ওয়ার্ড (\"Hey JARVIS\" / \"হে জার্ভিস\") এবং আপনার বাংলা ভয়েস কমান্ড শোনার জন্য প্রয়োজন। Boss নির্দেশ না দেওয়া পর্যন্ত কখনো গোপনে অডিও রেকর্ড করা হয় না।",
                        isGranted = uiState.hasMicPermission,
                        actionLabel = "অনুমতি দিন",
                        onActionClick = onRequestMicPermission,
                        testTag = "grant_mic_permission_button",
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionCard(
                            icon = Icons.Filled.Notifications,
                            title = "নোটিফিকেশন অনুমতি (POST_NOTIFICATIONS)",
                            explanation = "ব্যাকগ্রাউন্ডে JARVIS সার্ভিস চালু থাকলে স্ট্যাটাস ও Start/Stop নিয়ন্ত্রণ নোটিফিকেশনে দেখানোর জন্য প্রয়োজন।",
                            isGranted = uiState.hasNotificationPermission,
                            actionLabel = "অনুমতি দিন",
                            onActionClick = onRequestNotificationPermission,
                            testTag = "grant_notification_permission_button",
                        )
                    }

                    // Battery Optimization Guidance
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudSurface),
                        border = BorderStroke(1.dp, HudBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BatterySaver,
                                    contentDescription = null,
                                    tint = if (uiState.telemetry.isIgnoringBatteryOptimization) {
                                        StatusOnline
                                    } else {
                                        StatusWarning
                                    },
                                )
                                Text(
                                    text = "ব্যাটারি অপটিমাইজেশন নির্দেশনা",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MetallicSilver,
                                )
                            }
                            Text(
                                text = if (uiState.telemetry.isIgnoringBatteryOptimization) {
                                    "Boss, এই অ্যাপটি বর্তমানে ব্যাকগ্রাউন্ড সীমাবদ্ধতা মুক্ত (Unrestricted) রয়েছে। অ্যান্ড্রয়েড ভার্সন অনুমতি দিলে ব্যাকগ্রাউন্ড ওয়েক-ওয়ার্ড সবচেয়ে স্থিতিশীলভাবে চলবে।"
                                } else {
                                    "অ্যান্ড্রয়েড ব্যাটারি অপটিমাইজেশন ব্যাকগ্রাউন্ড মাইক্রোফোন সার্ভিস বন্ধ করে দিতে পারে। স্থিতিশীল ব্যাকগ্রাউন্ড কমান্ডের জন্য সেটিংস থেকে \"Ranadeep Sarkar\" অ্যাপের জন্য Unrestricted / Don't optimize নির্বাচন করুন।"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverMuted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenBatterySettings,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricBlue,
                                        contentColor = VoidBlack,
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("battery_optimization_button"),
                                ) {
                                    Text("ব্যাটারি সেটিংস")
                                }
                                OutlinedButton(
                                    onClick = onOpenAppSettings,
                                    border = BorderStroke(1.dp, HudBorder),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("app_settings_button"),
                                ) {
                                    Text("অ্যাপ সেটিংস", color = MetallicSilver)
                                }
                            }
                        }
                    }

                    SectionHeading("সহকারী ও ভয়েস সেটিংস (Preferences)")

                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudSurface),
                        border = BorderStroke(1.dp, HudBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ToggleSettingRow(
                                title = "ওয়েক-ওয়ার্ড শনাক্তকরণ",
                                subtitle = "\"Hey JARVIS\", \"হে জার্ভিস\", অথবা \"জার্ভিস\"",
                                checked = uiState.settings.wakeWordEnabled,
                                onCheckedChange = onWakeWordChanged,
                                testTag = "wake_word_switch",
                            )

                            ToggleSettingRow(
                                title = "বাংলা ভয়েস উত্তর (TTS)",
                                subtitle = uiState.ttsStatusLabel.ifBlank { "বাংলা TextToSpeech" },
                                checked = uiState.settings.voiceResponseEnabled,
                                onCheckedChange = onVoiceResponseChanged,
                                testTag = "voice_response_switch",
                            )

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = "কথা বলার গতি (TTS Speed)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MetallicSilver,
                                        )
                                    }
                                    Text(
                                        text = "${String.format("%.1f", uiState.settings.ttsSpeechRate)}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ArcCyanSoft,
                                    )
                                }
                                Slider(
                                    value = uiState.settings.ttsSpeechRate,
                                    onValueChange = onSpeechRateChanged,
                                    valueRange = 0.6f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricBlue,
                                        activeTrackColor = ElectricBlue,
                                        inactiveTrackColor = DeepSpace,
                                    ),
                                )
                            }

                            ToggleSettingRow(
                                title = "সংবেদনশীল কাজে নিরাপত্তা যাচাই",
                                subtitle = "মেসেজ, কল, ফাইল ডিলিট বা কেনাকাটার আগে Boss-এর অনুমতি চাইবে",
                                checked = uiState.settings.securityConfirmationRequired,
                                onCheckedChange = onSecurityConfirmationChanged,
                                testTag = "security_gate_switch",
                            )

                            ToggleSettingRow(
                                title = "রিস্টার্টের পর রিজিউম নোটিফিকেশন",
                                subtitle = "ডিভাইস চালু হলে JARVIS চালু করার জন্য এক-ট্যাপ নোটিফিকেশন দেখাবে",
                                checked = uiState.settings.autoNotifyOnBoot,
                                onCheckedChange = onAutoNotifyOnBootChanged,
                                testTag = "boot_notify_switch",
                            )
                        }
                    }

                    SectionHeading("সমর্থিত বাংলা কমান্ড তালিকা")

                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudSurface),
                        border = BorderStroke(1.dp, HudBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CommandReferenceLine("ওয়েক-ওয়ার্ড", "\"Hey JARVIS\" / \"হে জার্ভিস\" / \"জার্ভিস\"")
                            CommandReferenceLine("অ্যাপ খোলা", "ইউটিউব, ফেসবুক, ইনস্টাগ্রাম, হোয়াটসঅ্যাপ, সেটিংস, ক্যামেরা, গ্যালারি খোলো")
                            CommandReferenceLine("হার্ডওয়্যার", "টর্চ জ্বালাও / বন্ধ করো, ভলিউম বাড়াও / কমাও, ওয়াইফাই / ব্লুটুথ / ব্রাইটনেস / মোবাইল নেটওয়ার্ক")
                            CommandReferenceLine("সময় ও তথ্য", "এখন কয়টা বাজে?, আজ কী বার?, আজকের তারিখ কত?, ব্যাটারি কত শতাংশ?, ফোনের অবস্থা কী?")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = ElectricBlue,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    explanation: String,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    testTag: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HudSurface),
        border = BorderStroke(1.dp, if (isGranted) StatusOnline.copy(alpha = 0.45f) else HudBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) StatusOnline else StatusWarning,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MetallicSilver,
                    )
                }
                Icon(
                    imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isGranted) StatusOnline else StatusWarning,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = SilverMuted,
            )
            if (!isGranted) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = VoidBlack,
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(testTag),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MetallicSilver,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SilverMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBlack,
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = SilverMuted,
                uncheckedTrackColor = DeepSpace,
            ),
        )
    }
}

@Composable
private fun CommandReferenceLine(category: String, examples: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DeepSpace)
            .border(1.dp, HudBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            color = ElectricBlue,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = examples,
            style = MaterialTheme.typography.bodyMedium,
            color = MetallicSilver,
        )
    }
}
