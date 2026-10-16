package de.kalorien.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.kalorien.app.R
import de.kalorien.app.core.ui.theme.Tokens
import de.kalorien.app.feature.history.HistoryScreen
import de.kalorien.app.feature.settings.OwnFoodsScreen
import de.kalorien.app.feature.settings.SettingsScreen
import de.kalorien.app.feature.today.TodayScreen

/** Die drei Hauptbereiche der App. Neue Bereiche hier ergänzen. */
enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Today("today", R.string.nav_today, Icons.Filled.Home),
    History("history", R.string.nav_history, Icons.Filled.DateRange),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}

/** Unterseite der Einstellungen (nicht in der Leiste unten). */
private const val ROUTE_FOODS = "settings/foods"

@Composable
fun KalorienAppRoot() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val c = Tokens.colors

    fun goTo(dest: TopLevel) {
        nav.navigate(dest.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = c.surface) {
                TopLevel.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route || (dest == TopLevel.Settings && current == ROUTE_FOODS),
                        onClick = { goTo(dest) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.accent,
                            selectedTextColor = c.accent,
                            indicatorColor = c.accentSoft,
                            unselectedIconColor = c.muted,
                            unselectedTextColor = c.muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopLevel.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevel.Today.route) { TodayScreen(snackbar) }
            composable(TopLevel.History.route) {
                HistoryScreen(onOpenDay = { goTo(TopLevel.Today) })
            }
            composable(TopLevel.Settings.route) { SettingsScreen(snackbar, onOpenFoods = { nav.navigate(ROUTE_FOODS) }) }
            composable(ROUTE_FOODS) { OwnFoodsScreen(snackbar, onBack = { nav.popBackStack() }) }
        }
    }
}
