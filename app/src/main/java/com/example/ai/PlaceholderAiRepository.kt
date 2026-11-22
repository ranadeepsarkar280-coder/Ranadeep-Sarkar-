package com.example.ai

import kotlinx.coroutines.delay

/**
 * Placeholder AI repository used when Firebase AI Logic is not enabled.
 * Maintains in-memory session context and returns clearly labeled sample Bengali responses
 * addressing the owner respectfully as "Boss".
 */
class PlaceholderAiRepository : AiRepository {
    override val isCloudAiConnected: Boolean = false

    override val statusNotice: String =
        "Firebase AI সক্রিয় না থাকায় সাধারণ কথোপকথন নমুনা (Placeholder) মোডে চলছে।"

    private val sessionTopics = mutableListOf<String>()

    override suspend fun generateReply(
        userMessage: String,
        recentHistory: List<AiMessage>,
    ): AiResponse {
        delay(280L)
        val trimmed = userMessage.trim()
        sessionTopics.add(trimmed)
        val normalized = trimmed.lowercase()

        val previousTopicHint = recentHistory
            .lastOrNull { it.sender == AiMessage.Sender.BOSS && it.text.trim() != trimmed }
            ?.text
            ?.let { prev -> " (পূর্বের প্রসঙ্গ মনে রেখে: \"${prev.take(32)}\")" }
            .orEmpty()

        val reply = when {
            normalized.contains("কেমন আছ") || normalized.contains("how are you") ->
                "জি Boss, আমি সম্পূর্ণ প্রস্তুত এবং সচল আছি। আপনার নির্দেশের অপেক্ষায় রয়েছি।"

            normalized.contains("তোমার নাম") || normalized.contains("তুমি কে") || normalized.contains("who are you") ->
                "Boss, আমি জার্ভিস — রণদীপ সরকারের ব্যক্তিগত এআই সহকারী। আপনার প্রতিটি নির্দেশ পালনে আমি সর্বদা প্রস্তুত।"

            normalized.contains("ধন্যবাদ") || normalized.contains("thank") ->
                "আপনাকেও ধন্যবাদ, Boss। আর কোনো সাহায্য লাগলে নির্দ্বিধায় বলুন।"

            normalized.contains("হ্যালো") || normalized.contains("হাই") || normalized.contains("নমস্কার") || normalized.contains("hello") ->
                "নমস্কার Boss! বলুন, আজ আমি কীভাবে আপনাকে সাহায্য করতে পারি?"

            normalized.contains("সাহায্য") || normalized.contains("কি করতে পারো") || normalized.contains("help") ->
                "Boss, আমি বাংলা ভয়েস ওয়েক-ওয়ার্ড শুনতে পারি, ইউটিউব বা হোয়াটসঅ্যাপের মতো অ্যাপ খুলতে পারি, টর্চ ও ভলিউম নিয়ন্ত্রণ করতে পারি এবং সময়, তারিখ ও ব্যাটারির তথ্য জানাতে পারি।"

            else ->
                "[নমুনা AI উত্তর — Firebase সংযুক্ত নয়] Boss, আপনার প্রশ্ন \"$trimmed\" আমি নোট করেছি$previousTopicHint। লাইভ Gemini AI বুদ্ধিমত্তার জন্য Firebase AI Logic সক্রিয় করা প্রয়োজন; তবে আপনার সকল স্থানীয় কমান্ড, অ্যাপ লঞ্চ ও হার্ডওয়্যার নিয়ন্ত্রণ এখনই সম্পূর্ণ সচল রয়েছে।"
        }

        return AiResponse(
            text = reply,
            isPlaceholder = true,
        )
    }

    override fun resetSession() {
        sessionTopics.clear()
    }
}
