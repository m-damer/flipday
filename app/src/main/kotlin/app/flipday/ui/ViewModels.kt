package app.flipday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.flipday.AppContainer
import app.flipday.core.BackupCodec
import app.flipday.core.BackupContents
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventQueries
import app.flipday.core.EventQuery
import app.flipday.core.EventSort
import app.flipday.core.EventType
import app.flipday.core.NumeralMode
import app.flipday.data.AppSettings
import app.flipday.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class EventsViewModel(private val c: AppContainer) : ViewModel() {
    private val queryState = MutableStateFlow(EventQuery())
    val query: StateFlow<EventQuery> = queryState

    init {
        viewModelScope.launch {
            val days = c.settings.settings.first().archiveAfterDays
            c.repository.archiveFinished(LocalDate.now(), days)
        }
    }

    val events: StateFlow<List<Event>?> = combine(c.repository.observeEvents(), queryState) { all, q ->
        EventQueries.active(all, q, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Every event, ignoring the list's filter and sort, for the widget gallery's previews. */
    val allEvents: StateFlow<List<Event>> = c.repository.observeEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setType(t: EventType?) { queryState.value = queryState.value.copy(type = t) }
    fun apply(sort: EventSort, showCompleted: Boolean, type: EventType?) {
        queryState.value = queryState.value.copy(sort = sort, showCompleted = showCompleted, type = type)
    }
    fun reset() { queryState.value = EventQuery() }

    val undo: StateFlow<Event?> = c.pendingUndo

    fun restoreDeleted() {
        val e = c.pendingUndo.value ?: return
        c.pendingUndo.value = null
        viewModelScope.launch { c.repository.save(e) }
    }

    fun dismissUndo() { c.pendingUndo.value = null }
}

class SearchViewModel(private val c: AppContainer) : ViewModel() {
    val all: StateFlow<List<Event>> = c.repository.observeEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class ArchiveViewModel(private val c: AppContainer) : ViewModel() {
    private val type = MutableStateFlow<EventType?>(null)
    val selected: StateFlow<EventType?> = type
    val events: StateFlow<List<Event>?> = combine(c.repository.observeEvents(), type) { all, t -> EventQueries.archived(all, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setType(t: EventType?) { type.value = t }
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

    /** Stops a running count-up: it becomes a period that ends today. */
    fun markEnded(event: Event) { viewModelScope.launch { c.repository.save(EventCalculator.markEnded(event, LocalDate.now())) } }

    fun archive(event: Event) { viewModelScope.launch { c.repository.archive(event, LocalDate.now()) } }

    fun restore(event: Event) { viewModelScope.launch { c.repository.restore(event) } }

    fun duplicate(event: Event, newTitle: String, onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(c.repository.duplicate(event, newTitle)) }
    }

    fun repeatNextYear(event: Event) {
        viewModelScope.launch { c.repository.save(event.copy(date = event.date.plusYears(1), startDate = event.date)) }
    }
}

class BackupViewModel(private val c: AppContainer) : ViewModel() {
    suspend fun exportText(): String = BackupCodec.encode(c.repository.all(), LocalDate.now())

    /** Events from [contents] that are not already stored. */
    suspend fun newEvents(contents: BackupContents): List<Event> {
        val have = c.repository.all()
        return contents.events.filter { e -> have.none { BackupCodec.sameEvent(it, e) } }
    }

    suspend fun add(events: List<Event>) { events.forEach { c.repository.save(it.copy(id = 0)) } }
}

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun setNumerals(m: NumeralMode) { viewModelScope.launch { c.settings.setNumerals(m) } }
    fun setTheme(m: ThemeMode) { viewModelScope.launch { c.settings.setTheme(m) } }
    fun setNotifications(on: Boolean) { viewModelScope.launch { c.settings.setNotifications(on) } }
    fun markNotificationsAsked() { viewModelScope.launch { c.settings.markNotificationsAsked() } }
    fun setDefaultReminder(days: Int) { viewModelScope.launch { c.settings.setDefaultReminder(days) } }
    fun setArchiveAfter(days: Int) { viewModelScope.launch { c.settings.setArchiveAfter(days) } }
}

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        EventsViewModel::class.java -> EventsViewModel(c)
        SearchViewModel::class.java -> SearchViewModel(c)
        ArchiveViewModel::class.java -> ArchiveViewModel(c)
        EditorViewModel::class.java -> EditorViewModel(c)
        DetailViewModel::class.java -> DetailViewModel(c)
        SettingsViewModel::class.java -> SettingsViewModel(c)
        BackupViewModel::class.java -> BackupViewModel(c)
        else -> error("Unknown ViewModel ${modelClass.name}")
    } as T
}
