package app.flipday.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyAndNumeralTest {
    private val today = LocalDate.of(2026, 11, 12)
    private fun countdown(date: LocalDate, repeat: Boolean = false) = Event(title = "x", type = EventType.COUNTDOWN, date = date, repeatYearly = repeat)

    @Test fun archivesAfterSevenDays() {
        assertFalse(ArchivePolicy.shouldArchive(countdown(today.minusDays(7)), today))
        assertTrue(ArchivePolicy.shouldArchive(countdown(today.minusDays(8)), today))
    }
    @Test fun neverArchivesBirthdaysOrRepeating() {
        assertFalse(ArchivePolicy.shouldArchive(Event(title = "b", type = EventType.AGE, date = today.minusYears(5)), today))
        assertFalse(ArchivePolicy.shouldArchive(countdown(today.minusDays(30), repeat = true), today))
    }
    @Test fun offersRepeatForRecentlyFinished() {
        assertTrue(ArchivePolicy.shouldOfferRepeat(countdown(today.minusDays(1)), today))
        assertFalse(ArchivePolicy.shouldOfferRepeat(countdown(today.plusDays(1)), today))
    }
    @Test fun arabicIndicWhenLanguageIsArabic() =
        assertEquals("٢٣", NumeralFormatter.format("23", NumeralMode.MATCH_LANGUAGE, "ar"))
    @Test fun westernWhenForced() = assertEquals("23", NumeralFormatter.format("23", NumeralMode.WESTERN, "ar"))
    @Test fun arabicIndicWhenForcedInEnglish() = assertEquals("٠:٤٥ AM", NumeralFormatter.format("0:45 AM", NumeralMode.ARABIC_INDIC, "en-US"))
    @Test fun matchLanguageKeepsEnglish() = assertEquals("2,557", NumeralFormatter.format("2,557", NumeralMode.MATCH_LANGUAGE, "en"))
}
