package com.example.assistant

import java.util.Calendar

object BengaliFormatter {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bengaliDays = mapOf(
        Calendar.SUNDAY to "রবিবার",
        Calendar.MONDAY to "সোমবার",
        Calendar.TUESDAY to "মঙ্গলবার",
        Calendar.WEDNESDAY to "বুধবার",
        Calendar.THURSDAY to "বৃহস্পতিবার",
        Calendar.FRIDAY to "শুক্রবার",
        Calendar.SATURDAY to "শনিবার",
    )

    private val bengaliMonths = arrayOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর",
    )

    fun toBengaliDigits(number: Int): String = toBengaliDigits(number.toString())

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatCurrentTime(calendar: Calendar = Calendar.getInstance()): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val period = when (hour24) {
            in 4..5 -> "ভোর"
            in 6..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..18 -> "বিকাল"
            in 19..20 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val hourBn = toBengaliDigits(hour12)
        val minBn = toBengaliDigits(minute)
        return if (minute == 0) {
            "Boss, এখন $period ${hourBn}টা বাজে।"
        } else {
            "Boss, এখন $period ${hourBn}টা বেজে $minBn মিনিট।"
        }
    }

    fun formatCurrentDay(calendar: Calendar = Calendar.getInstance()): String {
        val day = bengaliDays[calendar.get(Calendar.DAY_OF_WEEK)] ?: "আজকের দিন"
        return "Boss, আজ $day।"
    }

    fun formatCurrentDate(calendar: Calendar = Calendar.getInstance()): String {
        val dayOfMonth = toBengaliDigits(calendar.get(Calendar.DAY_OF_MONTH))
        val month = bengaliMonths.getOrElse(calendar.get(Calendar.MONTH)) { "" }
        val year = toBengaliDigits(calendar.get(Calendar.YEAR))
        val dayName = bengaliDays[calendar.get(Calendar.DAY_OF_WEEK)] ?: ""
        return "Boss, আজ $dayOfMonth $month, $year ($dayName)।"
    }
}
