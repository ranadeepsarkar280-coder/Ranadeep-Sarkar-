package com.example.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class AppLauncher(private val context: Context) {

    fun launchKnownApp(app: KnownApp): String {
        return when (app) {
            KnownApp.SETTINGS -> launchIntent(
                Intent(Settings.ACTION_SETTINGS),
                "জি Boss, সেটিংস খুলে দিচ্ছি।",
                "সেটিংস",
            )
            KnownApp.CAMERA -> launchIntent(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                "জি Boss, ক্যামেরা খুলে দিচ্ছি।",
                "ক্যামেরা",
            )
            KnownApp.GALLERY -> launchGallery()
            KnownApp.YOUTUBE,
            KnownApp.FACEBOOK,
            KnownApp.INSTAGRAM,
            KnownApp.WHATSAPP -> {
                val pkg = app.packageName
                if (pkg != null) {
                    launchByPackage(pkg, app.displayNameBn)
                } else {
                    "Boss, ${app.displayNameBn} অ্যাপটি খোলা যায়নি।"
                }
            }
        }
    }

    fun launchByQuery(queryName: String): String {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = try {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying installed apps", e)
            emptyList()
        }
        val target = queryName.trim().lowercase()
        val match = apps.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            val pkg = resolveInfo.activityInfo.packageName.lowercase()
            label.contains(target) || pkg.contains(target)
        }
        if (match != null) {
            val pkgName = match.activityInfo.packageName
            val label = match.loadLabel(pm).toString()
            return launchByPackage(pkgName, label)
        }
        return "Boss, আপনার ফোনে \"$queryName\" অ্যাপটি ইনস্টল করা নেই।"
    }

    private fun launchByPackage(packageName: String, displayNameBn: String): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return "Boss, আপনার ফোনে $displayNameBn অ্যাপটি ইনস্টল করা নেই।"
        return launchIntent(
            launchIntent,
            "জি Boss, $displayNameBn খুলে দিচ্ছি।",
            displayNameBn,
        )
    }

    private fun launchGallery(): String {
        val galleryCandidates = listOf(
            Intent(Intent.ACTION_VIEW).apply { type = "image/*" },
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
        )
        for (intent in galleryCandidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                return try {
                    context.startActivity(intent)
                    "জি Boss, গ্যালারি খুলে দিচ্ছি।"
                } catch (e: Exception) {
                    Log.w(TAG, "Gallery intent failed", e)
                    continue
                }
            }
        }
        return "Boss, আপনার ফোনে গ্যালারি অ্যাপটি খুঁজে পাওয়া যায়নি।"
    }

    private fun launchIntent(intent: Intent, successMessage: String, appNameBn: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            successMessage
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $appNameBn", e)
            "Boss, $appNameBn অ্যাপটি খোলা সম্ভব হয়নি।"
        }
    }

    companion object {
        private const val TAG = "AppLauncher"
    }
}
