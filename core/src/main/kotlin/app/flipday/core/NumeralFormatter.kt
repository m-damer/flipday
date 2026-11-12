package app.flipday.core

enum class NumeralMode { MATCH_LANGUAGE, WESTERN, ARABIC_INDIC }

object NumeralFormatter {
    private const val ARABIC_INDIC_ZERO = '٠'

    /** Converts ASCII digits in [text] according to [mode]. [languageTag] is a BCP-47 tag such as "ar" or "en-US". */
    fun format(text: String, mode: NumeralMode, languageTag: String): String {
        val arabic = when (mode) {
            NumeralMode.WESTERN -> false
            NumeralMode.ARABIC_INDIC -> true
            NumeralMode.MATCH_LANGUAGE -> languageTag.substringBefore('-').equals("ar", ignoreCase = true)
        }
        if (!arabic) return text
        return buildString(text.length) { text.forEach { append(if (it in '0'..'9') ARABIC_INDIC_ZERO + (it - '0') else it) } }
    }
}
