package app.flipday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.flipday.AppContainer
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.core.NumeralMode
import app.flipday.core.ReminderRule
import app.flipday.data.AppSettings
import app.flipday.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class EventFilter { ALL, COUNTDOWN, BIRTHDAYS, COUNTUP }

class EventsViewModel(private val c: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(EventFilter.ALL)
    val selected: StateFlow<EventFilter> = filter

    val events: StateFlow<List<Event>?> = combine(c.repository.observeEvents(), filter) { all, f ->
        all.filter { it.archivedOn == null }.filter {
            when (f) {
                EventFilter.ALL -> true
                EventFilter.COUNTDOWN -> it.type == EventType.COUNTDOWN
                EventFilter.BIRTHDAYS -> it.type == EventType.AGE
                EventFilter.COUNTUP -> it.type == EventType.COUNTUP
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(f: EventFilter) { filter.value = f }
}

class AddEventViewModel(private val c: AppContainer) : ViewModel() {
    fun save(title: String, type: EventType, date: LocalDate, defaultReminderDays: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val reminders = if (type == EventType.COUNTUP || defaultReminderDays < 0) emptyList() else listOf(ReminderRule(defaultReminderDays))
            c.repository.save(Event(title = title.trim(), type = type, date = date, reminders = reminders))
            onDone()
        }
    }
}

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun setNumerals(m: NumeralMode) { viewModelScope.launch { c.settings.setNumerals(m) } }
    fun setTheme(m: ThemeMode) { viewModelScope.launch { c.settings.setTheme(m) } }
    fun setNotifications(on: Boolean) { viewModelScope.launch { c.settings.setNotifications(on) } }
}

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        EventsViewModel::class.java -> EventsViewModel(c)
        AddEventViewModel::class.java -> AddEventViewModel(c)
        SettingsViewModel::class.java -> SettingsViewModel(c)
        else -> error("Unknown ViewModel ${modelClass.name}")
    } as T
}
