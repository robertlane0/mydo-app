package com.mydo.app.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mydo.app.core.errors.AppResult
import com.mydo.app.di.AppContainer
import com.mydo.app.ui.components.MydoSnackbarHost
import com.mydo.app.ui.components.TaskComposerSheet
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.filters.FilterResultsScreen
import com.mydo.app.ui.filters.FilterResultsViewModel
import com.mydo.app.ui.filters.FiltersScreen
import com.mydo.app.ui.filters.FiltersViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.inbox.InboxScreen
import com.mydo.app.ui.labels.LabelDetailScreen
import com.mydo.app.ui.labels.LabelDetailViewModel
import com.mydo.app.ui.labels.LabelsScreen
import com.mydo.app.ui.labels.LabelsViewModel
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.notifications.NotificationsScreen
import com.mydo.app.ui.notifications.NotificationsViewModel
import com.mydo.app.ui.projects.ProjectsScreen
import com.mydo.app.ui.search.SearchScreen
import com.mydo.app.ui.search.SearchViewModel
import com.mydo.app.ui.settings.SettingsScreen
import com.mydo.app.ui.settings.SettingsViewModel
import com.mydo.app.ui.taskdetail.TaskDetailScreen
import com.mydo.app.ui.taskdetail.TaskDetailViewModel
import com.mydo.app.ui.today.TodayScreen
import com.mydo.app.ui.upcoming.UpcomingScreen
import com.mydo.app.ui.upcoming.UpcomingViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MydoApp(
    homeViewModel: HomeViewModel,
    taskComposerViewModel: TaskComposerViewModel,
    container: AppContainer,
    modifier: Modifier = Modifier,
    /** Set when MyDo was opened from a reminder notification's tap target
     *  (specs09-notifications.md, "Reminder Notifications" -> open). */
    deepLinkTaskId: UUID? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    var showTaskComposer by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(deepLinkTaskId) {
        deepLinkTaskId?.let { taskId ->
            navController.navigate(Screen.TaskDetail.createRoute(taskId.toString()))
            onDeepLinkConsumed()
        }
    }

    val unreadCount by androidx.compose.runtime.produceState(0) {
        container.observeUnreadNotificationCountUseCase().collect { result -> value = (result as? AppResult.Success)?.value ?: 0 }
    }
    val availableProjects by androidx.compose.runtime.produceState(emptyList<com.mydo.app.domain.model.Project>()) {
        container.observeActiveProjectsUseCase().collect { result -> value = (result as? AppResult.Success)?.value ?: emptyList() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "MyDo") },
                actions = {
                    Box {
                        Text(
                            "\uD83D\uDD14",
                            modifier = Modifier.padding(12.dp).clickable { navController.navigate(Screen.Notifications.route) },
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                            )
                        }
                    }
                    Box {
                        Text("\u22EE", modifier = Modifier.padding(12.dp).clickable { showOverflowMenu = true })
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(text = { Text("Labels") }, onClick = { showOverflowMenu = false; navController.navigate(Screen.Labels.route) })
                            DropdownMenuItem(text = { Text("Filters") }, onClick = { showOverflowMenu = false; navController.navigate(Screen.Filters.route) })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showOverflowMenu = false; navController.navigate(Screen.Settings.route) })
                        }
                    }
                },
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val screens = listOf(
                Screen.Inbox,
                Screen.Today,
                Screen.Upcoming,
                Screen.Projects,
                Screen.Search
            )

            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(screen.route.first().uppercase().toString()) },
                        label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTaskComposer = true }) {
                Text(text = "+")
            }
        },
        snackbarHost = { MydoSnackbarHost() },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Inbox.route) { InboxScreen(homeViewModel, navController, availableProjects) }
            composable(Screen.Today.route) { TodayScreen(navController) }
            composable(Screen.Upcoming.route) {
                val vm: UpcomingViewModel = viewModel(factory = UpcomingViewModel.Factory(container.observeUpcomingUseCase, container.rescheduleTaskUseCase, container.timeProvider))
                UpcomingScreen(
                    viewModel = vm,
                    composerViewModel = taskComposerViewModel,
                    navController = navController,
                    onRequestAddTask = { presetMillis ->
                        taskComposerViewModel.presetDueAtUtcMillis = presetMillis
                        showTaskComposer = true
                    },
                )
            }
            composable(Screen.Projects.route) { ProjectsScreen(navController) }
            composable(Screen.Search.route) {
                val vm: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(
                        container.searchUseCase,
                        container.observeRecentSearchesUseCase,
                        container.recordRecentSearchUseCase,
                        container.removeRecentSearchUseCase,
                        container.clearRecentSearchesUseCase,
                    )
                )
                SearchScreen(vm, navController)
            }
            composable(Screen.Labels.route) {
                val vm: LabelsViewModel = viewModel(
                    factory = LabelsViewModel.Factory(
                        container.observeLabelsUseCase, container.createLabelUseCase, container.updateLabelUseCase, container.deleteLabelUseCase,
                    )
                )
                LabelsScreen(vm, navController)
            }
            composable(Screen.LabelDetail.route) { backStackEntry ->
                val labelId = UUID.fromString(backStackEntry.arguments?.getString("labelId"))
                val vm: LabelDetailViewModel = viewModel(factory = LabelDetailViewModel.Factory(labelId, container.observeTasksForLabelUseCase))
                LabelDetailScreen(vm, navController)
            }
            composable(Screen.Filters.route) {
                val vm: FiltersViewModel = viewModel(
                    factory = FiltersViewModel.Factory(
                        container.observeFiltersUseCase, container.createFilterUseCase, container.updateFilterUseCase,
                        container.deleteFilterUseCase, container.toggleFilterFavoriteUseCase, container.validateFilterQueryUseCase,
                    )
                )
                FiltersScreen(vm, navController)
            }
            composable(Screen.FilterResults.route) { backStackEntry ->
                val filterId = UUID.fromString(backStackEntry.arguments?.getString("filterId"))
                val vm: FilterResultsViewModel = viewModel(factory = FilterResultsViewModel.Factory(filterId, container.filterRepository, container.runFilterUseCase))
                FilterResultsScreen(vm, navController)
            }
            composable(Screen.Notifications.route) {
                val vm: NotificationsViewModel = viewModel(
                    factory = NotificationsViewModel.Factory(
                        container.observeNotificationsUseCase, container.markNotificationReadUseCase,
                        container.markAllNotificationsReadUseCase, container.clearNotificationsUseCase,
                    )
                )
                NotificationsScreen(vm, navController)
            }
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.observeSettingsUseCase, container.updateSettingUseCase,
                        container.exportBackupUseCase, container.inspectBackupUseCase,
                        container.importBackupUseCase, container.clearLocalDataUseCase,
                        container.shareGateway, container.timeProvider,
                    )
                )
                SettingsScreen(vm)
            }
            composable(Screen.TaskDetail.route) { backStackEntry ->
                val taskId = UUID.fromString(backStackEntry.arguments?.getString("taskId"))
                val vm: TaskDetailViewModel = viewModel(
                    factory = TaskDetailViewModel.Factory(
                        taskId, container.observeTaskUseCase, container.observeActiveProjectsUseCase, container.observeLabelsUseCase,
                        container.observeRemindersUseCase, container.observeAttachmentsUseCase, container.updateTaskUseCase,
                        container.deleteTaskUseCase, container.completeTaskUseCase, container.undoCompleteTaskUseCase,
                        container.setRecurrenceUseCase, container.removeRecurrenceUseCase, container.skipNextOccurrenceUseCase,
                        container.rescheduleTaskUseCase, container.createAbsoluteReminderUseCase, container.createRelativeReminderUseCase,
                        container.deleteReminderUseCase, container.addAttachmentsUseCase, container.removeAttachmentUseCase,
                        container.assignLabelUseCase, container.unassignLabelUseCase, container.timeProvider,
                    )
                )
                TaskDetailScreen(taskViewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        if (showTaskComposer) {
            TaskComposerSheet(
                onDismiss = { showTaskComposer = false },
                viewModel = taskComposerViewModel
            )
        }
    }
}
