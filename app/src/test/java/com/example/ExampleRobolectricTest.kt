package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.CommandExecutor
import com.example.assistant.HardwareController
import com.example.assistant.AppLauncher
import com.example.ai.PlaceholderAiRepository
import com.example.data.JarvisSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("Offline build environment lacks cached Robolectric android-all artifact")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches Ranadeep Sarkar`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ranadeep Sarkar", appName)
    }

    @Test
    fun `console displays owner identity, RA orb, controls, and handles Bengali command`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithText("RANADEEP SARKAR").assertIsDisplayed()
        composeRule.onNodeWithTag("jarvis_core_orb").assertIsDisplayed()
        composeRule.onNodeWithTag("start_listening_button").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_listening_button").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_ai_notice_banner").assertIsDisplayed()

        composeRule.onNodeWithTag("command_input_field").performTextInput("এখন কয়টা বাজে?")
        composeRule.onNodeWithTag("send_command_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("এখন কয়টা বাজে?").assertIsDisplayed()
    }

    @Test
    fun `command executor checks security gate before sensitive action`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = CommandExecutor(
            appLauncher = AppLauncher(context),
            hardwareController = HardwareController(context),
            aiRepository = PlaceholderAiRepository(),
        )
        val outcome = executor.execute(
            rawInput = "মেসেজ পাঠাও",
            settings = JarvisSettings(securityConfirmationRequired = true),
            recentHistory = emptyList(),
        )
        assertTrue(outcome.requiresSecurityConfirmation != null)
        assertTrue(outcome.replyText.contains("Boss"))
    }
}
