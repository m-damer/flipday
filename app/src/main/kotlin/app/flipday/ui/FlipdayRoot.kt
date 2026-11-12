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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.flipday.R
import app.flipday.ui.events.AddEventScreen
import app.flipday.ui.events.EventsScreen
import app.flipday.ui.settings.AboutScreen
import app.flipday.ui.settings.DeveloperScreen
import app.flipday.ui.settings.SettingsScreen
import app.flipday.ui.settings.WidgetsScreen

object Routes {
    const val EVENTS = "events"; const val WIDGETS = "widgets"; const val SETTINGS = "settings"
    const val ADD = "add"; const val ABOUT = "about"; const val DEVELOPER = "developer"
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

    Scaffold(
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
            composable(Routes.EVENTS) { EventsScreen(factory) }
            composable(Routes.WIDGETS) { WidgetsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(factory, onAbout = { nav.navigate(Routes.ABOUT) }) }
            composable(Routes.ADD) { AddEventScreen(factory, onClose = { nav.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }, onDeveloper = { nav.navigate(Routes.DEVELOPER) }) }
            composable(Routes.DEVELOPER) { DeveloperScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
