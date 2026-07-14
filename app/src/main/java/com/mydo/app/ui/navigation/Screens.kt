package com.mydo.app.ui.navigation

sealed class Screen(val route: String) {
    object Inbox : Screen("inbox")
    object Today : Screen("today")
    object Upcoming : Screen("upcoming")
    object Projects : Screen("projects")
    object Search : Screen("search")
    object Labels : Screen("labels")
    object Filters : Screen("filters")
    object TaskDetail : Screen("taskDetail/{taskId}") {
        fun createRoute(taskId: String) = "taskDetail/$taskId"
    }
}
