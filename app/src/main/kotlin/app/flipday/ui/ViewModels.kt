package app.flipday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.flipday.AppContainer
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.core.NumeralMode
import app.flipday.data.AppSettings
import app.flipday.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val undo: StateFlow<Event?> = c.pendingUndo

    fun restoreDeleted() {
        val e = c.pendingUndo.value ?: return
        c.pendingUndo.value = null
        viewModelScope.launch { c.repository.save(e) }
    }

    fun dismissUndo() { c.pendingUndo.value = null }
}

class EditorViewModel(private val c: AppContainer) : ViewModel() {
    suspend fun load(id: Long): Event? = c.repository.get(id)

    fun save(event: Event, onDone: () -> Unit) {
        viewModelScope.launch {
            c.repository.save(event)
            onDone()
        }
    }
}

class DetailViewModel(private val c: AppContainer) : ViewModel() {
    fun event(id: Long): Flow<Event?> = c.repository.observe(id)

    /** Deletes and keeps the event for a short Undo. */
    fun delete(event: Event, onDone: () -> Unit) {
        viewModelScope.launch {
            c.repository.delete(event)
            c.pendingUndo.value = event
            onDone()
        }
    }

    fun repeatNextYear(event: Event) {
        viewModelScope.launch { c.repository.save(event.copy(date = event.date.plusYears(1), startDate = event.date)) }
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
        EditorViewModel::class.java -> EditorViewModel(c)
        DetailViewModel::class.java -> DetailViewModel(c)
        SettingsViewModel::class.java -> SettingsViewModel(c)
        else -> error("Unknown ViewModel ${modelClass.name}")
    } as T
}
