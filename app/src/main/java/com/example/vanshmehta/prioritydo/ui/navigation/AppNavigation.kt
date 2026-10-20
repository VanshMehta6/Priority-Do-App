@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vanshmehta.prioritydo.ui.navigation

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vanshmehta.prioritydo.data.prefs.SettingsStore
import com.example.vanshmehta.prioritydo.data.prefs.ThemeMode
import com.example.vanshmehta.prioritydo.ui.components.LocalReduceMotion
import com.example.vanshmehta.prioritydo.ui.components.SkeletonList
import com.example.vanshmehta.prioritydo.ui.components.TaskActions
import com.example.vanshmehta.prioritydo.ui.screens.auth.LoginScreen
import com.example.vanshmehta.prioritydo.ui.screens.auth.RegisterScreen
import com.example.vanshmehta.prioritydo.ui.screens.auth.SplashScreen
import com.example.vanshmehta.prioritydo.ui.screens.home.HomeScreen
import com.example.vanshmehta.prioritydo.ui.screens.insights.InsightsScreen
import com.example.vanshmehta.prioritydo.ui.screens.profile.ProfileScreen
import com.example.vanshmehta.prioritydo.ui.screens.tasks.TaskDetailScreen
import com.example.vanshmehta.prioritydo.ui.screens.tasks.TaskFormScreen
import com.example.vanshmehta.prioritydo.ui.screens.tasks.TasksScreen
import com.example.vanshmehta.prioritydo.ui.screens.timer.TimerScreen
import com.example.vanshmehta.prioritydo.util.rememberNotificationPermissionRequester
import com.example.vanshmehta.prioritydo.viewmodel.AuthViewModel
import com.example.vanshmehta.prioritydo.viewmodel.TaskViewModel
import com.example.vanshmehta.prioritydo.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TASKS = "tasks?filter={filter}"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"
    const val TIMER = "timer?taskId={taskId}"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val DETAIL = "detail/{id}"
    fun tasks(filter: String = "ALL") = "tasks?filter=$filter"
    fun timer(taskId: String? = null) = if (taskId == null) "timer" else "timer?taskId=$taskId"
    fun edit(id: String) = "edit/$id"
    fun detail(id: String) = "detail/$id"
}

private data class BottomItem(val base: String, val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem("home", Routes.HOME, "Home", Icons.Default.Home),
    BottomItem("tasks", Routes.tasks(), "Tasks", Icons.Default.Checklist),
    BottomItem("insights", Routes.INSIGHTS, "Insights", Icons.Default.Insights),
    BottomItem("profile", Routes.PROFILE, "Profile", Icons.Default.Person)
)

@Composable
fun AppNavigation(settings: SettingsStore, themeMode: ThemeMode) {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val taskVm: TaskViewModel = viewModel()
    val timerVm: TimerViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val auth by authVm.state.collectAsStateWithLifecycle()
    val ui by taskVm.ui.collectAsStateWithLifecycle()
    val saving by taskVm.saving.collectAsStateWithLifecycle()
    val timer by timerVm.state.collectAsStateWithLifecycle()
    // Respect the system "remove animations" setting.
    val reduce = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }

    val profile = auth.profile
    val snackbar = remember { SnackbarHostState() }
    val requestNotif = rememberNotificationPermissionRequester()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Load tasks for the signed-in user only. On logout all listeners stop and state is cleared.
    LaunchedEffect(auth.user?.uid) {
        taskVm.setUser(auth.user?.uid)
        if (auth.user == null) timerVm.clear()
    }

    // Session check -> login/home routing.
    LaunchedEffect(auth.checking, auth.user?.uid, currentRoute) {
        if (auth.checking) return@LaunchedEffect
        val route = currentRoute ?: return@LaunchedEffect // nav graph not attached yet
        if (auth.user == null) {
            if (route != Routes.LOGIN && route != Routes.REGISTER) {
                nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
            }
        } else if (route == Routes.SPLASH || route == Routes.LOGIN || route == Routes.REGISTER) {
            nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }
    // Snackbars (with Undo for deletes).
    LaunchedEffect(Unit) {
        taskVm.events.collect { e ->
            val r = snackbar.showSnackbar(e.text, actionLabel = if (e.undoable) "Undo" else null, withDismissAction = true, duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed && e.undoable) taskVm.undoDelete()
        }
    }

    val showBottom = currentRoute != null && bottomItems.any { currentRoute.startsWith(it.base) }
    val showFab = currentRoute != null && (currentRoute.startsWith("home") || currentRoute.startsWith("tasks"))

    val actions = TaskActions(
        onToggle = { taskVm.toggleComplete(it) },
        onDelete = { taskVm.deleteTask(it) },
        onOpen = { nav.navigate(Routes.detail(it.id)) },
        onEdit = { nav.navigate(Routes.edit(it.id)) }
    )

    CompositionLocalProvider(LocalReduceMotion provides reduce) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (showFab) FloatingActionButton(
                    onClick = { nav.navigate(Routes.ADD) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, contentDescription = "Add task") }
            },
            bottomBar = {
                if (showBottom) NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route?.startsWith(item.base) == true } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTop(nav, item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { pad ->
            val enter: EnterTransition = if (reduce) EnterTransition.None else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }
            val exit: ExitTransition = if (reduce) ExitTransition.None else fadeOut(tween(160))
            val popEnter: EnterTransition = if (reduce) EnterTransition.None else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 14 }
            val popExit: ExitTransition = if (reduce) ExitTransition.None else fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 14 }

            NavHost(
                navController = nav, startDestination = Routes.SPLASH,
                modifier = Modifier.padding(pad).then(if (showBottom) Modifier else Modifier.navigationBarsPadding()),
                enterTransition = { enter }, exitTransition = { exit }, popEnterTransition = { popEnter }, popExitTransition = { popExit }
            ) {
                composable(Routes.SPLASH) { SplashScreen() }
                composable(Routes.LOGIN) {
                    LoginScreen(auth, authVm::login, authVm::resetPassword, { nav.navigate(Routes.REGISTER) }, authVm::clearMessages)
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(auth, authVm::register, { nav.popBackStack() }, authVm::clearMessages)
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        profile = profile, ui = ui, actions = actions,
                        onOpenFilter = { nav.navigate(Routes.tasks(it)) },
                        onOpenInsights = { navigateTop(nav, Routes.INSIGHTS) },
                        onOpenTimer = { nav.navigate(Routes.timer()) },
                        onOpenProfile = { navigateTop(nav, Routes.PROFILE) },
                        onQuickAdd = taskVm::quickAdd,
                        onAddTask = { nav.navigate(Routes.ADD) }
                    )
                }
                composable(
                    Routes.TASKS,
                    arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "ALL" })
                ) { entry ->
                    TasksScreen(ui, entry.arguments?.getString("filter") ?: "ALL", actions)
                }
                composable(Routes.INSIGHTS) { InsightsScreen(ui) }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        profile = profile, themeMode = themeMode,
                        onThemeMode = { m -> scope.launch { settings.setThemeMode(m) } },
                        onUpdateName = authVm::updateDisplayName,
                        onLogout = { authVm.logout() }
                    )
                }
                composable(Routes.ADD) {
                    TaskFormScreen(null, saving, { taskVm.saveTask(it) { nav.popBackStack() } }, { nav.popBackStack() })
                }
                composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id")
                    val task = ui.tasks.firstOrNull { it.id == id }
                    when {
                        task != null -> TaskFormScreen(task, saving, { taskVm.saveTask(it) { nav.popBackStack() } }, { nav.popBackStack() })
                        ui.loading -> SkeletonList()
                        else -> TaskDetailScreen(null, { nav.popBackStack() }, {}, {}, {}, {})
                    }
                }
                composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id")
                    val task = ui.tasks.firstOrNull { it.id == id }
                    if (task == null && ui.loading) SkeletonList()
                    else TaskDetailScreen(
                        task, { nav.popBackStack() }, { nav.navigate(Routes.edit(it.id)) }, taskVm::toggleComplete,
                        { taskVm.deleteTask(it); nav.popBackStack() },
                        { nav.navigate(Routes.timer(it.id)) }
                    )
                }
                composable(
                    Routes.TIMER,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { entry ->
                    val tid = entry.arguments?.getString("taskId")
                    TimerScreen(
                        state = timer, tasks = ui.tasks,
                        presetTask = ui.tasks.firstOrNull { it.id == tid },
                        onBack = { nav.popBackStack() },
                        onConfigure = { timerVm.configure(it) },
                        onLink = { t -> timerVm.link(t?.id, t?.title) },
                        onStart = timerVm::start, onPause = timerVm::pause, onReset = timerVm::reset,
                        requestNotificationPermission = requestNotif
                    )
                }
            }
        }
    }
}

private fun navigateTop(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}
