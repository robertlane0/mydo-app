package com.mydo.app.ui.navigation

sealed class Screen(val route: String) {
    object Inbox : Screen("inbox")
    object Today : Screen("today")
    object Upcoming : Screen("upcoming")
    object Projects : Screen("projects")
    object Search : Screen("search")
    object Labels : Screen("labels")
    object LabelDetail : Screen("labels/{labelId}") {
        fun createRoute(labelId: String) = "labels/$labelId"
    }
    object Filters : Screen("filters")
    object FilterResults : Screen("filters/{filterId}") {
        fun createRoute(filterId: String) = "filters/$filterId"
    }
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object TaskDetail : Screen("taskDetail/{taskId}") {
        fun createRoute(taskId: String) = "taskDetail/$taskId"
    }
}
