package app.flipday.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Per-widget settings, stored in Glance's own per-widget preferences state. */
object WidgetKeys {
    /** 0 or missing means "nearest upcoming event". */
    val EVENT_ID = longPreferencesKey("event_id")
    val TEMPLATE = stringPreferencesKey("template")
}

/** FLAP: flap tiles on a card. PAPER: teal card with plain numerals. */
enum class WidgetTemplate { FLAP, PAPER;
    companion object { fun parse(s: String?): WidgetTemplate = entries.firstOrNull { it.name == s } ?: FLAP }
}
