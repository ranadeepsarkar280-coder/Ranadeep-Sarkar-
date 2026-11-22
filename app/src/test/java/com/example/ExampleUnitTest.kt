package com.example

import com.example.ai.AiMessage
import com.example.ai.PlaceholderAiRepository
import com.example.assistant.BengaliCommandParser
import com.example.assistant.BengaliFormatter
import com.example.assistant.JarvisRuntimeHub
import com.example.assistant.KnownApp
import com.example.assistant.ParsedCommand
import com.example.assistant.PendingSecurityConfirmation
import com.example.assistant.SensitiveActionType
import com.example.assistant.VolumeAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `wake word detection recognizes English and Bengali wake phrases`() {
        assertTrue(BengaliCommandParser.containsWakeWord("Hey JARVIS"))
        assertTrue(BengaliCommandParser.containsWakeWord("হে জার্ভিস"))
        assertTrue(BengaliCommandParser.containsWakeWord("জার্ভিস"))
        assertEquals(ParsedCommand.WakeWordOnly, BengaliCommandParser.parse("হে জার্ভিস"))
        assertEquals(ParsedCommand.WakeWordOnly, BengaliCommandParser.parse("Hey JARVIS"))
    }

    @Test
    fun `app launch commands parse YouTube, Facebook, Instagram, WhatsApp, Settings, Camera, Gallery`() {
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.YOUTUBE),
            BengaliCommandParser.parse("হে জার্ভিস, ইউটিউব খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.FACEBOOK),
            BengaliCommandParser.parse("হে জার্ভিস, ফেসবুক খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.INSTAGRAM),
            BengaliCommandParser.parse("হে জার্ভিস, ইনস্টাগ্রাম খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.WHATSAPP),
            BengaliCommandParser.parse("হে জার্ভিস, হোয়াটসঅ্যাপ খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.SETTINGS),
            BengaliCommandParser.parse("হে জার্ভিস, সেটিংস খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.CAMERA),
            BengaliCommandParser.parse("হে জার্ভিস, ক্যামেরা খোলো।"),
        )
        assertEquals(
            ParsedCommand.LaunchKnownApp(KnownApp.GALLERY),
            BengaliCommandParser.parse("হে জার্ভিস, গ্যালারি খোলো।"),
        )
    }

    @Test
    fun `hardware and info commands parse accurately`() {
        assertEquals(
            ParsedCommand.ToggleFlashlight(true),
            BengaliCommandParser.parse("টর্চ জ্বালাও"),
        )
        assertEquals(
            ParsedCommand.ToggleFlashlight(false),
            BengaliCommandParser.parse("ফ্ল্যাশলাইট বন্ধ করো"),
        )
        assertEquals(
            ParsedCommand.AdjustVolume(VolumeAction.UP),
            BengaliCommandParser.parse("ভলিউম বাড়াও"),
        )
        assertEquals(
            ParsedCommand.AdjustVolume(VolumeAction.DOWN),
            BengaliCommandParser.parse("ভলিউম কমাও"),
        )
        assertEquals(
            ParsedCommand.OpenWifiSettings,
            BengaliCommandParser.parse("ওয়াইফাই সেটিংস খোলো"),
        )
        assertEquals(
            ParsedCommand.OpenBluetoothSettings,
            BengaliCommandParser.parse("ব্লুটুথ সেটিংস"),
        )
        assertEquals(
            ParsedCommand.OpenBrightnessSettings,
            BengaliCommandParser.parse("ব্রাইটনেস কমাও"),
        )
        assertEquals(
            ParsedCommand.OpenMobileNetworkSettings,
            BengaliCommandParser.parse("মোবাইল নেটওয়ার্ক সেটিংস"),
        )
        assertEquals(
            ParsedCommand.QueryTime,
            BengaliCommandParser.parse("জার্ভিস এখন কয়টা বাজে?"),
        )
        assertEquals(
            ParsedCommand.QueryDay,
            BengaliCommandParser.parse("আজ কী বার?"),
        )
        assertEquals(
            ParsedCommand.QueryDate,
            BengaliCommandParser.parse("আজকের তারিখ কত?"),
        )
        assertEquals(
            ParsedCommand.QueryBattery,
            BengaliCommandParser.parse("আমার ফোনের ব্যাটারি কত শতাংশ?"),
        )
        assertEquals(
            ParsedCommand.QueryPhoneStatus,
            BengaliCommandParser.parse("আমার ফোনের অবস্থা কী?"),
        )
    }

    @Test
    fun `sensitive actions trigger security confirmation gate`() {
        val parsed = BengaliCommandParser.parse("মেসেজ পাঠাও")
        assertTrue(parsed is ParsedCommand.SensitiveOperation)
        assertEquals(
            SensitiveActionType.SEND_MESSAGE,
            (parsed as ParsedCommand.SensitiveOperation).type,
        )

        val deleteParsed = BengaliCommandParser.parse("সব ফাইল ডিলিট করো")
        assertTrue(deleteParsed is ParsedCommand.SensitiveOperation)
        assertEquals(
            SensitiveActionType.DELETE_FILES,
            (deleteParsed as ParsedCommand.SensitiveOperation).type,
        )
    }

    @Test
    fun `bengali formatter formats numerals, time, day, and date`() {
        assertEquals("২৫%", "${BengaliFormatter.toBengaliDigits(25)}%")

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 22)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        val timeText = BengaliFormatter.formatCurrentTime(cal)
        assertTrue(timeText.contains("Boss"))
        assertTrue(timeText.contains("১০টা বেজে ৩০ মিনিট"))

        val dateText = BengaliFormatter.formatCurrentDate(cal)
        assertTrue(dateText.contains("২২ নভেম্বর, ২০২৬"))
    }

    @Test
    fun `placeholder ai repository addresses Boss respectfully and retains context`() = runTest {
        val repo = PlaceholderAiRepository()
        val first = repo.generateReply("বাংলাদেশের রাজধানী কী?", emptyList())
        assertTrue(first.isPlaceholder)
        assertTrue(first.text.contains("Boss"))

        val history = listOf(
            AiMessage("1", AiMessage.Sender.BOSS, "বাংলাদেশের রাজধানী কী?"),
            AiMessage("2", AiMessage.Sender.JARVIS, first.text, isPlaceholderResponse = true),
        )
        val second = repo.generateReply("আরও বলো", history)
        assertTrue(second.text.contains("Boss"))
        assertTrue(second.text.contains("বাংলাদেশের রাজধানী"))
    }

    @Test
    fun `runtime hub manages service state and security confirmation honestly`() {
        JarvisRuntimeHub.setServiceRunning(true)
        JarvisRuntimeHub.setBackgroundListening(true)
        assertTrue(JarvisRuntimeHub.isServiceRunning.value)
        assertTrue(JarvisRuntimeHub.isBackgroundListening.value)

        // When OS destroys service, both serviceRunning and backgroundListening reset to false
        JarvisRuntimeHub.setServiceRunning(false)
        assertTrue(!JarvisRuntimeHub.isServiceRunning.value)
        assertTrue(!JarvisRuntimeHub.isBackgroundListening.value)

        val pending = PendingSecurityConfirmation(
            actionType = SensitiveActionType.MAKE_PURCHASE,
            rawCommand = "অনলাইনে অর্ডার করো",
            promptBn = "Boss অনুমতি দিন",
        )
        JarvisRuntimeHub.requestSecurityConfirmation(pending)
        assertEquals(pending, JarvisRuntimeHub.pendingConfirmation.value)
        JarvisRuntimeHub.requestSecurityConfirmation(null)
        assertNull(JarvisRuntimeHub.pendingConfirmation.value)
    }
}
