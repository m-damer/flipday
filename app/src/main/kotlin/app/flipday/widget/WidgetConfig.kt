package app.flipday.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
    val DYNAMIC = booleanPreferencesKey("dynamic")
    val BG_MODE = stringPreferencesKey("bg_mode")
    val RADIUS = intPreferencesKey("radius")
    val NUMBER_SIZE = stringPreferencesKey("number_size")
    val FONT = stringPreferencesKey("font")
    val NUMERALS = stringPreferencesKey("numerals")
    val TIME_MODE = stringPreferencesKey("time_mode")
}

/**
 * The eight widget families of the design (board 06, widgets). Each one is also its own launcher widget, so the
 * launcher picker lists them by name. Older saved values map to the nearest family: FLAP and PAPER became [BIG].
 */
enum class WidgetTemplate(val hasSmall: Boolean) {
    BIG(true), AGE(true), COUNTUP(true), AGENDA(false), RING(true), DATE(true), TICKER(false), YEAR(false);

    companion object {
        fun parse(s: String?): WidgetTemplate = when (s) {
            null, "FLAP", "PAPER" -> BIG
            else -> entries.firstOrNull { it.name == s } ?: BIG
        }
    }
}

/** Widget size classes: strip 4x1, small 2x2, medium 4x2, large 4x4 launcher cells. */
enum class WidgetSize { STRIP, SMALL, MEDIUM, LARGE }

/** Accent colours for the flap digits and highlights. Teal is the brand colour; the others are studio choices. */
enum class WidgetAccent(val bgDay: Long, val bgNight: Long, val digit: Long) {
    TEAL(0xFF0F766E, 0xFF4FD1B5, 0xFF5EE0C4),
    INDIGO(0xFF3730A3, 0xFFA5B4FC, 0xFFC7D2FE),
    ROSE(0xFFBE123C, 0xFFFDA4AF, 0xFFFECDD3),
    AMBER(0xFFB45309, 0xFFFCD34D, 0xFFFDE68A);

    companion object { fun parse(s: String?): WidgetAccent = entries.firstOrNull { it.name == s } ?: TEAL }
}

/** Family picked in the gallery, used as the starting family of the next widget that gets set up. */
object WidgetDraft { @Volatile var template: WidgetTemplate = WidgetTemplate.BIG }
