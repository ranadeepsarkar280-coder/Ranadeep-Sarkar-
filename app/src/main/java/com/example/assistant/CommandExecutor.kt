package com.example.assistant

import com.example.ai.AiMessage
import com.example.ai.AiRepository
import com.example.data.JarvisSettings

data class CommandExecutionOutcome(
    val replyText: String,
    val isWakeOnly: Boolean = false,
    val isPlaceholderAi: Boolean = false,
    val requiresSecurityConfirmation: PendingSecurityConfirmation? = null,
)

class CommandExecutor(
    private val appLauncher: AppLauncher,
    private val hardwareController: HardwareController,
    private val aiRepository: AiRepository,
) {
    suspend fun execute(
        rawInput: String,
        settings: JarvisSettings,
        recentHistory: List<AiMessage>,
    ): CommandExecutionOutcome {
        return when (val parsed = BengaliCommandParser.parse(rawInput)) {
            is ParsedCommand.WakeWordOnly -> CommandExecutionOutcome(
                replyText = "জি Boss, বলুন।",
                isWakeOnly = true,
            )

            is ParsedCommand.SensitiveOperation -> {
                if (settings.securityConfirmationRequired) {
                    val prompt =
                        "Boss, \"${parsed.rawCommand}\" একটি সংবেদনশীল কাজ (${parsed.type.labelBn})। নিরাপত্তা নীতি অনুযায়ী আপনার স্পষ্ট অনুমতি ছাড়া আমি এটি সম্পন্ন করব না। আপনি কি নিশ্চিতভাবে এগিয়ে যেতে চান?"
                    CommandExecutionOutcome(
                        replyText = prompt,
                        requiresSecurityConfirmation = PendingSecurityConfirmation(
                            actionType = parsed.type,
                            rawCommand = parsed.rawCommand,
                            promptBn = prompt,
                        ),
                    )
                } else {
                    CommandExecutionOutcome(
                        replyText = "Boss, নিরাপত্তার স্বার্থে সংবেদনশীল কাজটি সরাসরি স্বয়ংক্রিয়ভাবে চালানো হয়নি। অনুগ্রহ করে সংশ্লিষ্ট অ্যাপ থেকে এটি সম্পন্ন করুন।"
                    )
                }
            }

            is ParsedCommand.LaunchKnownApp -> CommandExecutionOutcome(
                replyText = appLauncher.launchKnownApp(parsed.app)
            )

            is ParsedCommand.LaunchCustomApp -> CommandExecutionOutcome(
                replyText = appLauncher.launchByQuery(parsed.queryName)
            )

            is ParsedCommand.ToggleFlashlight -> CommandExecutionOutcome(
                replyText = hardwareController.setFlashlight(parsed.turnOn)
            )

            is ParsedCommand.AdjustVolume -> CommandExecutionOutcome(
                replyText = hardwareController.adjustVolume(parsed.action)
            )

            is ParsedCommand.OpenBrightnessSettings -> CommandExecutionOutcome(
                replyText = hardwareController.openDisplayBrightnessSettings()
            )

            is ParsedCommand.OpenWifiSettings -> CommandExecutionOutcome(
                replyText = hardwareController.openWifiSettings()
            )

            is ParsedCommand.OpenBluetoothSettings -> CommandExecutionOutcome(
                replyText = hardwareController.openBluetoothSettings()
            )

            is ParsedCommand.OpenMobileNetworkSettings -> CommandExecutionOutcome(
                replyText = hardwareController.openMobileNetworkSettings()
            )

            is ParsedCommand.QueryTime -> CommandExecutionOutcome(
                replyText = BengaliFormatter.formatCurrentTime()
            )

            is ParsedCommand.QueryDay -> CommandExecutionOutcome(
                replyText = BengaliFormatter.formatCurrentDay()
            )

            is ParsedCommand.QueryDate -> CommandExecutionOutcome(
                replyText = BengaliFormatter.formatCurrentDate()
            )

            is ParsedCommand.QueryBattery -> CommandExecutionOutcome(
                replyText = hardwareController.getBatteryResponseBn()
            )

            is ParsedCommand.QueryPhoneStatus -> CommandExecutionOutcome(
                replyText = hardwareController.getPhoneStatusResponseBn()
            )

            is ParsedCommand.GeneralConversation -> {
                val response = aiRepository.generateReply(parsed.prompt, recentHistory)
                CommandExecutionOutcome(
                    replyText = response.text,
                    isPlaceholderAi = response.isPlaceholder,
                )
            }
        }
    }

    fun resolveSecurityConfirmation(
        confirmation: PendingSecurityConfirmation,
        approved: Boolean,
    ): String {
        return if (approved) {
            "জি Boss, আপনার অনুমতি গ্রহণ করা হয়েছে। নিরাপত্তার কারণে চূড়ান্ত প্রেরণ বা পেমেন্ট নিশ্চিত করতে অনুগ্রহ করে সংশ্লিষ্ট অ্যাপে পদক্ষেপটি সম্পন্ন করুন।"
        } else {
            "ঠিক আছে Boss, আপনার নির্দেশ অনুযায়ী \"${confirmation.actionType.labelBn}\" বাতিল করা হয়েছে।"
        }
    }
}
