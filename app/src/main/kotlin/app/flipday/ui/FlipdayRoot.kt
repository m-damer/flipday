package app.flipday.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRail
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.flipday.R
import app.flipday.ui.events.ArchiveScreen
import app.flipday.ui.events.DetailScreen
import app.flipday.ui.events.SearchScreen
import app.flipday.ui.events.EditorScreen
import app.flipday.ui.events.EventsScreen
import app.flipday.ui.settings.AboutScreen
import app.flipday.ui.settings.DeveloperScreen
import app.flipday.ui.settings.BackupScreen
import app.flipday.ui.settings.LicensesScreen
import app.flipday.ui.settings.PrivacyScreen
import app.flipday.ui.settings.SettingsScreen
import app.flipday.ui.settings.WidgetsScreen

object Routes {
    const val EVENTS = "events"; const val WIDGETS = "widgets"; const val SETTINGS = "settings"
    const val ADD = "add"; const val SEARCH = "search"; const val ARCHIVE = "archive"; const val EDIT = "edit/{id}"; const val DETAIL = "detail/{id}"; const val ABOUT = "about"; const val BACKUP = "backup"; const val PRIVACY = "privacy"; const val LICENSES = "licenses"; const val DEVELOPER = "developer"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.EVENTS, R.string.tab_events, Icons.Outlined.CalendarMonth),
    Tab(Routes.WIDGETS, R.string.tab_widgets, Icons.Outlined.Widgets),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Outlined.Settings),
)

/**
 * Three tabs (Events, Widgets, Settings). Tab roots have no back arrow and keep the bottom bar.
 * Child screens (add, about, developer) hide the bar and show Back or Close. FAB only on Events.
 */
@Composable
fun FlipdayRoot(factory: ViewModelProvider.Factory, openEventId: Long? = null, onOpened: () -> Unit = {}, openUpdate: Boolean = false, onUpdateOpened: () -> Unit = {}) {
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val appSettings by settingsVm.settings.collectAsState()
    if (!appSettings.onboarded) { OnboardingScreen(appSettings.numerals, onStart = { settingsVm.markOnboarded() }); return }
    val nav = rememberNavController()
    val widthDp = LocalConfiguration.current.screenWidthDp
    val useRail = widthDp >= 600
    val twoPane = widthDp >= 840
    var selectedEvent by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(openUpdate) {
        if (openUpdate) { nav.navigate(Routes.SETTINGS) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }; onUpdateOpened() }
    }
    LaunchedEffect(openEventId) { openEventId?.let { nav.navigate("detail/$it"); onOpened() } }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = tabs.any { it.route == route }
    val eventsVm: EventsViewModel = viewModel(factory = factory)
    val undo by eventsVm.undo.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.deleted_snack)
    val undoText = stringResource(R.string.undo)
    LaunchedEffect(undo) {
        if (undo != null) {
            val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Long, withDismissAction = false)
            if (result == SnackbarResult.ActionPerformed) eventsVm.restoreDeleted() else eventsVm.dismissUndo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (route == Routes.EVENTS) {
                FloatingActionButton(
                    onClick = { nav.navigate(Routes.ADD) }, shape = MaterialTheme.shapes.small,
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                }
            }
        },
        bottomBar = {
            if (onTab && !useRail) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Row(Modifier.padding(padding)) {
        if (onTab && useRail) NavigationRail {
            tabs.forEach { t ->
                NavigationRailItem(
                    selected = route == t.route,
                    onClick = {
                        nav.navigate(t.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    icon = { Icon(t.icon, contentDescription = null) },
                    label = { Text(stringResource(t.label)) },
                )
            }
        }
        NavHost(nav, startDestination = Routes.EVENTS, modifier = Modifier.weight(1f)) {
            composable(Routes.EVENTS) {
                if (twoPane) Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(0.4f)) {
                        EventsScreen(factory, onOpen = { selectedEvent = it }, onSearch = { nav.navigate(Routes.SEARCH) }, onArchive = { nav.navigate(Routes.ARCHIVE) }, selectedId = selectedEvent, onPreset = { nav.navigate("${Routes.ADD}?preset=$it") })
                    }
                    VerticalDivider()
                    Box(Modifier.weight(0.6f)) {
                        val id = selectedEvent
                        if (id != null) DetailScreen(
                            factory, id, onBack = { selectedEvent = null }, onEdit = { e -> nav.navigate("edit/$e") },
                            onOpenEvent = { n -> selectedEvent = n }, showBack = false, onWidget = { nav.navigate(Routes.WIDGETS) },
                        )
                    }
                } else EventsScreen(factory, onOpen = { nav.navigate("detail/$it") }, onSearch = { nav.navigate(Routes.SEARCH) }, onArchive = { nav.navigate(Routes.ARCHIVE) }, onPreset = { nav.navigate("${Routes.ADD}?preset=$it") })
            }
            composable(Routes.WIDGETS) { WidgetsScreen(factory) }
            composable(Routes.SETTINGS) { SettingsScreen(factory, onAbout = { nav.navigate(Routes.ABOUT) }, onBackup = { nav.navigate(Routes.BACKUP) }) }
            composable(Routes.SEARCH) { SearchScreen(factory, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("detail/$it") }) }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(factory, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("detail/$it") }, onChangeArchiveTime = {
                    nav.navigate(Routes.SETTINGS) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                })
            }
            composable("${Routes.ADD}?preset={preset}", arguments = listOf(navArgument("preset") { type = NavType.StringType; nullable = true; defaultValue = null })) {
                EditorScreen(factory, eventId = null, onClose = { nav.popBackStack() }, preset = it.arguments?.getString("preset"))
            }
            composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                EditorScreen(factory, eventId = it.arguments?.getLong("id"), onClose = { nav.popBackStack() })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("id") ?: 0L
                DetailScreen(factory, id, onBack = { nav.popBackStack() }, onEdit = { e -> nav.navigate("edit/$e") }, onOpenEvent = { n -> nav.navigate("detail/$n") }, onWidget = { nav.navigate(Routes.WIDGETS) })
            }
            composable(Routes.BACKUP) { BackupScreen(factory, onBack = { nav.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }, onDeveloper = { nav.navigate(Routes.DEVELOPER) }, onPrivacy = { nav.navigate(Routes.PRIVACY) }, onLicenses = { nav.navigate(Routes.LICENSES) }) }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.LICENSES) { LicensesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DEVELOPER) { DeveloperScreen(onBack = { nav.popBackStack() }) }
        }
        }
    }
}
