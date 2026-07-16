package com.mydo.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Notification
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.theme.MydoSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        when (val state = uiState) {
            NotificationsUiState.Loading -> MydoLoadingState(message = "Loading\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            is NotificationsUiState.Error -> MydoErrorState(title = "Unable to load notifications", message = state.message, modifier = Modifier.fillMaxSize().padding(padding))
            is NotificationsUiState.Ready -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.notifications.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(MydoSpacing.screenMargin),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Mark all read", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { viewModel.markAllRead() })
                            Text("Clear all", color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { viewModel.clearAll() })
                        }
                    }
                    if (state.notifications.isEmpty()) {
                        MydoEmptyState(title = "No notifications", message = "Reminders and system messages will show up here.", modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(contentPadding = PaddingValues(horizontal = MydoSpacing.screenMargin)) {
                            items(state.notifications, key = { it.id }) { notification ->
                                NotificationRow(notification, onClick = {
                                    viewModel.markRead(notification.id)
                                    notification.taskId?.let { navController.navigate("taskDetail/$it") }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(vertical = MydoSpacing.small),
    ) {
        Column {
            Text(notification.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = Instant.ofEpochMilli(notification.createdAtUtcMillis).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
