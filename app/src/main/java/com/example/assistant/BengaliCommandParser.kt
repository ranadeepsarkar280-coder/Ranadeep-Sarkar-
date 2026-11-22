package com.example.assistant

enum class KnownApp(
    val displayNameBn: String,
    val packageName: String?,
) {
    YOUTUBE("ইউটিউব", "com.google.android.youtube"),
    FACEBOOK("ফেসবুক", "com.facebook.katana"),
    INSTAGRAM("ইনস্টাগ্রাম", "com.instagram.android"),
    WHATSAPP("হোয়াটসঅ্যাপ", "com.whatsapp"),
    SETTINGS("সেটিংস", null),
    CAMERA("ক্যামেরা", null),
    GALLERY("গ্যালারি", null),
}

enum class VolumeAction {
    UP,
    DOWN,
    MUTE,
    MAX,
}

enum class SensitiveActionType(val labelBn: String) {
    SEND_MESSAGE("মেসেজ পাঠানো"),
    DELETE_FILES("ফাইল মুছে ফেলা"),
    MAKE_PURCHASE("কেনাকাটা বা পেমেন্ট"),
    MAKE_CALL("ফোন কল করা"),
}

sealed interface ParsedCommand {
    data object WakeWordOnly : ParsedCommand
    data class LaunchKnownApp(val app: KnownApp) : ParsedCommand
    data class LaunchCustomApp(val queryName: String) : ParsedCommand
    data class ToggleFlashlight(val turnOn: Boolean) : ParsedCommand
    data class AdjustVolume(val action: VolumeAction) : ParsedCommand
    data object OpenBrightnessSettings : ParsedCommand
    data object OpenWifiSettings : ParsedCommand
    data object OpenBluetoothSettings : ParsedCommand
    data object OpenMobileNetworkSettings : ParsedCommand
    data object QueryTime : ParsedCommand
    data object QueryDay : ParsedCommand
    data object QueryDate : ParsedCommand
    data object QueryBattery : ParsedCommand
    data object QueryPhoneStatus : ParsedCommand
    data class SensitiveOperation(
        val type: SensitiveActionType,
        val rawCommand: String,
    ) : ParsedCommand
    data class GeneralConversation(val prompt: String) : ParsedCommand
}

object BengaliCommandParser {

    private val wakePrefixes = listOf(
        "hey jarvis",
        "hi jarvis",
        "ok jarvis",
        "হে জার্ভিস",
        "হে জারভিস",
        "এই জার্ভিস",
        "জার্ভিস",
        "জারভিস",
        "jarvis",
    )

    fun containsWakeWord(rawInput: String): Boolean {
        val normalized = normalize(rawInput)
        return wakePrefixes.any { prefix ->
            normalized == prefix ||
                normalized.startsWith("$prefix ") ||
                normalized.startsWith("$prefix,") ||
                normalized.contains(prefix)
        }
    }

    fun stripWakeWord(rawInput: String): String {
        var cleaned = rawInput.trim()
        val lower = cleaned.lowercase()
        for (prefix in wakePrefixes) {
            if (lower.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length)
                    .trimStart(',', ' ', ':', '-', '।', '.', '!')
                break
            }
        }
        return cleaned.trim()
    }

    fun parse(rawInput: String): ParsedCommand {
        val stripped = stripWakeWord(rawInput)
        if (stripped.isBlank()) {
            return ParsedCommand.WakeWordOnly
        }
        val text = normalize(stripped)

        // 1. Security-sensitive actions always intercepted first
        detectSensitiveAction(text)?.let { sensitiveType ->
            return ParsedCommand.SensitiveOperation(sensitiveType, stripped)
        }

        // 2. Flashlight ON / OFF
        if (isFlashlightCommand(text)) {
            val turnOff = text.contains("বন্ধ") ||
                text.contains("অফ") ||
                text.contains("নিভাও") ||
                text.contains("off")
            return ParsedCommand.ToggleFlashlight(turnOn = !turnOff)
        }

        // 3. Volume controls
        if (text.contains("ভলিউম") || text.contains("আওয়াজ") || text.contains("সাউন্ড") || text.contains("volume") || text.contains("sound") || text.contains("মিউট") || text.contains("mute")) {
            return when {
                text.contains("মিউট") || text.contains("সাইলেন্ট") || text.contains("mute") || text.contains("বন্ধ") ->
                    ParsedCommand.AdjustVolume(VolumeAction.MUTE)
                text.contains("ফুল") || text.contains("সর্বোচ্চ") || text.contains("max") ->
                    ParsedCommand.AdjustVolume(VolumeAction.MAX)
                text.contains("কমা") || text.contains("কম") || text.contains("down") || text.contains("decrease") ->
                    ParsedCommand.AdjustVolume(VolumeAction.DOWN)
                else ->
                    ParsedCommand.AdjustVolume(VolumeAction.UP)
            }
        }

        // 4. Hardware settings shortcuts
        if (text.contains("ব্রাইটনেস") || text.contains("brightness") || text.contains("ডিসপ্লের আলো") || text.contains("স্ক্রিন আলো")) {
            return ParsedCommand.OpenBrightnessSettings
        }
        if (text.contains("ওয়াইফাই") || text.contains("ওয়াই-ফাই") || text.contains("wifi") || text.contains("wi-fi")) {
            return ParsedCommand.OpenWifiSettings
        }
        if (text.contains("ব্লুটুথ") || text.contains("bluetooth")) {
            return ParsedCommand.OpenBluetoothSettings
        }
        if (text.contains("মোবাইল নেটওয়ার্ক") || text.contains("নেটওয়ার্ক সেটিংস") || text.contains("মোবাইল ডেটা") || text.contains("mobile network") || text.contains("mobile data")) {
            return ParsedCommand.OpenMobileNetworkSettings
        }

        // 5. Time & System Information
        if (text.contains("কয়টা বাজে") || text.contains("সময় কত") || text.contains("এখন সময়") || text.contains("what time") || text.contains("current time")) {
            return ParsedCommand.QueryTime
        }
        if (text.contains("কী বার") || text.contains("কি বার") || text.contains("আজ বার") || text.contains("what day")) {
            return ParsedCommand.QueryDay
        }
        if (text.contains("তারিখ") || text.contains("date")) {
            return ParsedCommand.QueryDate
        }
        if (text.contains("ব্যাটারি") || text.contains("চার্জ কত") || text.contains("battery")) {
            return ParsedCommand.QueryBattery
        }
        if (text.contains("ফোনের অবস্থা") || text.contains("সিস্টেম অবস্থা") || text.contains("ফোন স্ট্যাটাস") || text.contains("phone status") || text.contains("device status")) {
            return ParsedCommand.QueryPhoneStatus
        }

        // 6. Known App Launches
        detectKnownApp(text)?.let { app ->
            return ParsedCommand.LaunchKnownApp(app)
        }

        // 7. Generic app open pattern: "<app> খোলো" / "<app> ওপেন করো" / "open <app>"
        val openTriggers = listOf("খোলো", "খুলুন", "খোল", "ওপেন করো", "চালু করো", "open ")
        if (openTriggers.any { text.contains(it) }) {
            val candidate = text
                .replace("খোলো", "")
                .replace("খুলুন", "")
                .replace("খোল", "")
                .replace("ওপেন করো", "")
                .replace("চালু করো", "")
                .replace("open ", "")
                .replace("অ্যাপ", "")
                .trim()
            if (candidate.isNotBlank()) {
                return ParsedCommand.LaunchCustomApp(candidate)
            }
        }

        // 8. General Bengali/English AI conversation
        return ParsedCommand.GeneralConversation(stripped)
    }

    private fun detectSensitiveAction(text: String): SensitiveActionType? = when {
        text.contains("মেসেজ পাঠা") || text.contains("এসএমএস পাঠা") || text.contains("send message") || text.contains("send sms") ->
            SensitiveActionType.SEND_MESSAGE
        text.contains("ডিলিট") || text.contains("মুছে ফেল") || text.contains("delete file") || text.contains("delete") ->
            SensitiveActionType.DELETE_FILES
        text.contains("কেনাকাটা") || text.contains("পেমেন্ট") || text.contains("অর্ডার করো") || text.contains("purchase") || text.contains("buy ") || text.contains("pay ") ->
            SensitiveActionType.MAKE_PURCHASE
        text.contains("কল করো") || text.contains("ফোন করো") || text.contains("make a call") || text.contains("call ") ->
            SensitiveActionType.MAKE_CALL
        else -> null
    }

    private fun isFlashlightCommand(text: String): Boolean {
        return text.contains("টর্চ") ||
            text.contains("ফ্ল্যাশলাইট") ||
            text.contains("ফ্ল্যাশ") ||
            text.contains("flashlight") ||
            text.contains("torch") ||
            (text.contains("লাইট") && (text.contains("অন") || text.contains("অফ") || text.contains("জ্বালাও") || text.contains("বন্ধ")))
    }

    private fun detectKnownApp(text: String): KnownApp? = when {
        text.contains("ইউটিউব") || text.contains("youtube") -> KnownApp.YOUTUBE
        text.contains("ফেসবুক") || text.contains("facebook") -> KnownApp.FACEBOOK
        text.contains("ইনস্টাগ্রাম") || text.contains("instagram") -> KnownApp.INSTAGRAM
        text.contains("হোয়াটসঅ্যাপ") || text.contains("হোয়াটস অ্যাপ") || text.contains("whatsapp") -> KnownApp.WHATSAPP
        text.contains("সেটিংস") || text.contains("সেটিং") || text.contains("settings") -> KnownApp.SETTINGS
        text.contains("ক্যামেরা") || text.contains("camera") -> KnownApp.CAMERA
        text.contains("গ্যালারি") || text.contains("গ্যালারী") || text.contains("gallery") || text.contains("photos") -> KnownApp.GALLERY
        else -> null
    }

    private fun normalize(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace("।", "")
            .replace(",", " ")
            .replace("?", "")
            .replace("!", "")
            .replace(Regex("\\s+"), " ")
    }
}
