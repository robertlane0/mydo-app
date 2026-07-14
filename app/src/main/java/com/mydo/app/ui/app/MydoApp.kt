package com.mydo.app.ui.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mydo.app.ui.components.MydoSnackbarHost
import com.mydo.app.ui.components.TaskComposerSheet
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.inbox.InboxScreen
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.projects.ProjectsScreen
import com.mydo.app.ui.search.SearchScreen
import com.mydo.app.ui.taskdetail.TaskDetailScreen
import com.mydo.app.ui.today.TodayScreen
import com.mydo.app.ui.upcoming.UpcomingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MydoApp(
    homeViewModel: HomeViewModel,
    taskComposerViewModel: TaskComposerViewModel,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var showTaskComposer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = "MyDo") })
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
            composable(Screen.Inbox.route) { InboxScreen(homeViewModel, navController) }
            composable(Screen.Today.route) { TodayScreen(navController) }
            composable(Screen.Upcoming.route) { UpcomingScreen(navController) }
            composable(Screen.Projects.route) { ProjectsScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.TaskDetail.route) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                TaskDetailScreen(taskId = taskId, onBack = { navController.popBackStack() })
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
