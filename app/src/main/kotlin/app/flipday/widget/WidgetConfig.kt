package app.flipday.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Per-widget settings, stored in Glance's own per-widget preferences state. */
object WidgetKeys {
    /** 0 or missing means "nearest upcoming event". */
    val EVENT_ID = longPreferencesKey("event_id")
    val TEMPLATE = stringPreferencesKey("template")
    val ACCENT = stringPreferencesKey("accent")
    val SHOW_TITLE = booleanPreferencesKey("show_title")
    val SHOW_UNIT = booleanPreferencesKey("show_unit")
}

/** FLAP: flap tiles on a card. PAPER: teal card with plain numerals. */
enum class WidgetTemplate { FLAP, PAPER;
    companion object { fun parse(s: String?): WidgetTemplate = entries.firstOrNull { it.name == s } ?: FLAP }
}

/**
 * Widget colour choices (studio, Colors tab). [bg] is the PAPER card (day, night); [digit] is the flap digit colour.
 * All pairs keep white text above 4.5:1 on [bgDay].
 */
enum class WidgetAccent(val bgDay: Long, val bgNight: Long, val digit: Long) {
    TEAL(0xFF0F766E, 0xFF0B4F49, 0xFF5EE0C4),
    INDIGO(0xFF3730A3, 0xFF2B2670, 0xFFC7D2FE),
    ROSE(0xFFBE123C, 0xFF7F1230, 0xFFFECDD3),
    AMBER(0xFFB45309, 0xFF7A3A06, 0xFFFDE68A);

    companion object { fun parse(s: String?): WidgetAccent = entries.firstOrNull { it.name == s } ?: TEAL }
}

/** Style picked in the gallery, used as the starting style of the next widget that gets set up. */
object WidgetDraft { @Volatile var template: WidgetTemplate = WidgetTemplate.FLAP }
