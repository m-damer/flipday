package app.flipday.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import app.flipday.ui.settings.SettingsScreen
import app.flipday.ui.settings.WidgetsScreen

object Routes {
    const val EVENTS = "events"; const val WIDGETS = "widgets"; const val SETTINGS = "settings"
    const val ADD = "add"; const val SEARCH = "search"; const val ARCHIVE = "archive"; const val EDIT = "edit/{id}"; const val DETAIL = "detail/{id}"; const val ABOUT = "about"; const val DEVELOPER = "developer"
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
fun FlipdayRoot(factory: ViewModelProvider.Factory) {
    val nav = rememberNavController()
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
                FloatingActionButton(onClick = { nav.navigate(Routes.ADD) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                }
            }
        },
        bottomBar = {
            if (onTab) NavigationBar {
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
        NavHost(nav, startDestination = Routes.EVENTS, modifier = Modifier.padding(padding)) {
            composable(Routes.EVENTS) { EventsScreen(factory, onOpen = { nav.navigate("detail/$it") }, onSearch = { nav.navigate(Routes.SEARCH) }, onArchive = { nav.navigate(Routes.ARCHIVE) }) }
            composable(Routes.WIDGETS) { WidgetsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(factory, onAbout = { nav.navigate(Routes.ABOUT) }) }
            composable(Routes.SEARCH) { SearchScreen(factory, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("detail/$it") }) }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(factory, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("detail/$it") }, onChangeArchiveTime = {
                    nav.navigate(Routes.SETTINGS) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                })
            }
            composable(Routes.ADD) { EditorScreen(factory, eventId = null, onClose = { nav.popBackStack() }) }
            composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                EditorScreen(factory, eventId = it.arguments?.getLong("id"), onClose = { nav.popBackStack() })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("id") ?: 0L
                DetailScreen(factory, id, onBack = { nav.popBackStack() }, onEdit = { e -> nav.navigate("edit/$e") }, onOpenEvent = { n -> nav.navigate("detail/$n") })
            }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }, onDeveloper = { nav.navigate(Routes.DEVELOPER) }) }
            composable(Routes.DEVELOPER) { DeveloperScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
