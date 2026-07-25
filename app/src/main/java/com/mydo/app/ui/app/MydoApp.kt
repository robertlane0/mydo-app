package com.mydo.app.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mydo.app.core.errors.AppResult
import com.mydo.app.di.AppContainer
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.Project
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
import com.mydo.app.ui.projects.ProjectDetailScreen
import com.mydo.app.ui.projects.ProjectDetailViewModel
import com.mydo.app.ui.projects.ProjectsScreen
import com.mydo.app.ui.projects.ProjectsViewModel
import com.mydo.app.ui.search.SearchScreen
import com.mydo.app.ui.search.SearchViewModel
import com.mydo.app.ui.settings.SettingsScreen
import com.mydo.app.ui.settings.SettingsViewModel
import com.mydo.app.ui.taskdetail.TaskDetailScreen
import com.mydo.app.ui.taskdetail.TaskDetailViewModel
import com.mydo.app.ui.today.TodayScreen
import com.mydo.app.ui.today.TodayViewModel
import com.mydo.app.ui.upcoming.UpcomingScreen
import com.mydo.app.ui.upcoming.UpcomingViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(Screen.Inbox, "Inbox", Icons.Filled.Inbox),
    BottomNavItem(Screen.Today, "Today", Icons.Filled.Today),
    BottomNavItem(Screen.Upcoming, "Upcoming", Icons.Filled.Upcoming),
    BottomNavItem(Screen.Projects, "Projects", Icons.Filled.Folder),
    BottomNavItem(Screen.Search, "Search", Icons.Filled.Search),
)

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

    LaunchedEffect(deepLinkTaskId) {
        deepLinkTaskId?.let { taskId ->
            navController.navigate(Screen.TaskDetail.createRoute(taskId.toString()))
            onDeepLinkConsumed()
        }
    }

    val unreadCount by produceState(0) {
        container.observeUnreadNotificationCountUseCase().collect { result -> value = (result as? AppResult.Success)?.value ?: 0 }
    }
    val availableProjects by produceState(emptyList<Project>()) {
        container.observeActiveProjectsUseCase().collect { result -> value = (result as? AppResult.Success)?.value ?: emptyList() }
    }
    val availableLabels by produceState(emptyList<Label>()) {
        container.observeLabelsUseCase().collect { result -> value = (result as? AppResult.Success)?.value ?: emptyList() }
    }

    /** Opens the global Quick Add sheet with the given context preset (specs12-user-flows.md). */
    fun requestAddTask(dueAtUtcMillis: Long? = null, projectId: UUID? = null, sectionId: UUID? = null) {
        taskComposerViewModel.presetDueAtUtcMillis = dueAtUtcMillis
        taskComposerViewModel.presetProjectId = projectId
        taskComposerViewModel.presetSectionId = sectionId
        showTaskComposer = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "MyDo") },
                actions = {
                    Box {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
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

            NavigationBar {
                BOTTOM_NAV_ITEMS.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { requestAddTask() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { MydoSnackbarHost() },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Inbox.route) {
                InboxScreen(
                    homeViewModel = homeViewModel,
                    navController = navController,
                    availableProjects = availableProjects,
                    availableLabels = availableLabels,
                    onAddTask = { requestAddTask() },
                )
            }
            composable(Screen.Today.route) {
                val vm: TodayViewModel = viewModel(
                    factory = TodayViewModel.Factory(
                        container.observeTodayTasksUseCase, container.timeProvider, container.completeTaskUseCase,
                        container.undoCompleteTaskUseCase, container.reorderTasksUseCase, container.bulkSetPriorityUseCase,
                        container.bulkSetDueDateUseCase, container.bulkMoveTasksUseCase, container.bulkCompleteTasksUseCase,
                        container.bulkDeleteTasksUseCase, container.undoBulkTaskOperationUseCase, container.bulkAddLabelsUseCase,
                        container.undoBulkAddLabelsUseCase,
                    ),
                )
                TodayScreen(
                    viewModel = vm,
                    navController = navController,
                    availableProjects = availableProjects,
                    availableLabels = availableLabels,
                    onAddTask = {
                        val todayNoon = LocalDate.now().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        requestAddTask(dueAtUtcMillis = todayNoon)
                    },
                )
            }
            composable(Screen.Upcoming.route) {
                val vm: UpcomingViewModel = viewModel(
                    factory = UpcomingViewModel.Factory(
                        container.observeUpcomingUseCase, container.rescheduleTaskUseCase, container.completeTaskUseCase,
                        container.undoCompleteTaskUseCase, container.timeProvider,
                    ),
                )
                UpcomingScreen(
                    viewModel = vm,
                    navController = navController,
                    onRequestAddTask = { presetMillis -> requestAddTask(dueAtUtcMillis = presetMillis) },
                )
            }
            composable(Screen.Projects.route) {
                val vm: ProjectsViewModel = viewModel(
                    factory = ProjectsViewModel.Factory(
                        container.observeActiveProjectsUseCase, container.observeArchivedProjectsUseCase, container.createProjectUseCase,
                        container.updateProjectUseCase, container.setProjectArchivedUseCase, container.toggleProjectFavoriteUseCase,
                        container.deleteProjectUseCase, container.countActiveTasksInProjectUseCase, container.reorderProjectsUseCase,
                    ),
                )
                ProjectsScreen(vm, navController)
            }
            composable(Screen.ProjectDetail.route) { backStackEntry ->
                val projectId = UUID.fromString(backStackEntry.arguments?.getString("projectId"))
                val vm: ProjectDetailViewModel = viewModel(
                    key = "project-detail-$projectId",
                    factory = ProjectDetailViewModel.Factory(
                        projectId, container.observeProjectUseCase, container.observeSectionsUseCase, container.observeProjectTasksUseCase,
                        container.createSectionUseCase, container.renameSectionUseCase, container.deleteSectionUseCase,
                        container.reorderSectionsUseCase, container.updateProjectUseCase, container.deleteProjectUseCase,
                        container.completeTaskUseCase, container.undoCompleteTaskUseCase, container.reorderTasksUseCase,
                        container.bulkSetPriorityUseCase, container.bulkSetDueDateUseCase, container.bulkMoveTasksUseCase,
                        container.bulkCompleteTasksUseCase, container.bulkDeleteTasksUseCase, container.undoBulkTaskOperationUseCase,
                        container.bulkAddLabelsUseCase, container.undoBulkAddLabelsUseCase,
                    ),
                )
                ProjectDetailScreen(
                    viewModel = vm,
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    availableProjects = availableProjects,
                    availableLabels = availableLabels,
                    onAddTask = { sectionId -> requestAddTask(projectId = projectId, sectionId = sectionId) },
                )
            }
            composable(Screen.Search.route) {
                val vm: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(
                        container.searchUseCase,
                        container.observeRecentSearchesUseCase,
                        container.recordRecentSearchUseCase,
                        container.removeRecentSearchUseCase,
                        container.clearRecentSearchesUseCase,
                        container.completeTaskUseCase,
                        container.undoCompleteTaskUseCase,
                    ),
                )
                SearchScreen(vm, navController)
            }
            composable(Screen.Labels.route) {
                val vm: LabelsViewModel = viewModel(
                    factory = LabelsViewModel.Factory(
                        container.observeLabelsUseCase, container.createLabelUseCase, container.updateLabelUseCase, container.deleteLabelUseCase,
                    ),
                )
                LabelsScreen(vm, navController)
            }
            composable(Screen.LabelDetail.route) { backStackEntry ->
                val labelId = UUID.fromString(backStackEntry.arguments?.getString("labelId"))
                val vm: LabelDetailViewModel = viewModel(
                    key = "label-detail-$labelId",
                    factory = LabelDetailViewModel.Factory(
                        labelId, container.observeTasksForLabelUseCase, container.completeTaskUseCase, container.undoCompleteTaskUseCase,
                    ),
                )
                LabelDetailScreen(vm, navController)
            }
            composable(Screen.Filters.route) {
                val vm: FiltersViewModel = viewModel(
                    factory = FiltersViewModel.Factory(
                        container.observeFiltersUseCase, container.createFilterUseCase, container.updateFilterUseCase,
                        container.deleteFilterUseCase, container.toggleFilterFavoriteUseCase, container.validateFilterQueryUseCase,
                    ),
                )
                FiltersScreen(vm, navController)
            }
            composable(Screen.FilterResults.route) { backStackEntry ->
                val filterId = UUID.fromString(backStackEntry.arguments?.getString("filterId"))
                val vm: FilterResultsViewModel = viewModel(
                    key = "filter-results-$filterId",
                    factory = FilterResultsViewModel.Factory(
                        filterId, container.filterRepository, container.runFilterUseCase, container.completeTaskUseCase, container.undoCompleteTaskUseCase,
                    ),
                )
                FilterResultsScreen(vm, navController)
            }
            composable(Screen.Notifications.route) {
                val vm: NotificationsViewModel = viewModel(
                    factory = NotificationsViewModel.Factory(
                        container.observeNotificationsUseCase, container.markNotificationReadUseCase,
                        container.markAllNotificationsReadUseCase, container.clearNotificationsUseCase,
                    ),
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
                    ),
                )
                SettingsScreen(vm)
            }
            composable(Screen.TaskDetail.route) { backStackEntry ->
                val taskId = UUID.fromString(backStackEntry.arguments?.getString("taskId"))
                val vm: TaskDetailViewModel = viewModel(
                    key = "task-detail-$taskId",
                    factory = TaskDetailViewModel.Factory(
                        taskId, container.observeTaskUseCase, container.observeActiveProjectsUseCase, container.observeLabelsUseCase,
                        container.observeRemindersUseCase, container.observeAttachmentsUseCase, container.updateTaskUseCase,
                        container.deleteTaskUseCase, container.completeTaskUseCase, container.undoCompleteTaskUseCase,
                        container.setRecurrenceUseCase, container.removeRecurrenceUseCase, container.skipNextOccurrenceUseCase,
                        container.rescheduleTaskUseCase, container.createAbsoluteReminderUseCase, container.createRelativeReminderUseCase,
                        container.deleteReminderUseCase, container.addAttachmentsUseCase, container.removeAttachmentUseCase,
                        container.assignLabelUseCase, container.unassignLabelUseCase, container.timeProvider,
                    ),
                )
                TaskDetailScreen(taskViewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        if (showTaskComposer) {
            TaskComposerSheet(
                onDismiss = { showTaskComposer = false },
                viewModel = taskComposerViewModel,
                availableProjects = availableProjects,
            )
        }
    }
}
