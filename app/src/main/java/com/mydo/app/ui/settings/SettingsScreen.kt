package com.mydo.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mydo.app.BuildConfig
import com.mydo.app.domain.model.AppSettings
import com.mydo.app.domain.model.DefaultHomeView
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.StartOfWeek
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        SettingsUiState.Loading -> MydoLoadingState(message = "Loading settings\u2026", modifier = Modifier.fillMaxSize())
        is SettingsUiState.Error -> MydoErrorState(title = "Unable to load settings", message = state.message, modifier = Modifier.fillMaxSize())
        is SettingsUiState.Ready -> SettingsContent(state.settings, viewModel)
    }
}

@Composable
private fun SettingsContent(settings: AppSettings, vm: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MydoSpacing.screenMargin),
    ) {
        SettingsSection("General") {
            EnumRow("Start screen", settings.defaultHomeView, DefaultHomeView.entries, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, vm::setDefaultHomeView)
            EnumRow("Default priority", settings.defaultPriority, Priority.entries, { it.name }, vm::setDefaultPriority)
            EnumRow("Week starts on", settings.startOfWeek, StartOfWeek.entries, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, vm::setStartOfWeek)
        }
        SettingsSection("Appearance") {
            EnumRow("Theme", settings.themeMode, ThemeMode.entries, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, vm::setThemeMode)
            ToggleRow("Use dynamic color", settings.useDynamicColor, vm::setUseDynamicColor)
            ToggleRow("Compact task rows", settings.compactTaskRows, vm::setCompactTaskRows)
        }
        SettingsSection("Productivity") {
            ToggleRow("Show completed tasks", settings.showCompletedTasks, vm::setShowCompletedTasks)
            ToggleRow("Show weekends in Upcoming", settings.showWeekends, vm::setShowWeekends)
            ToggleRow("Completion animation", settings.completionAnimationEnabled, vm::setCompletionAnimationEnabled)
        }
        SettingsSection("Notifications") {
            ToggleRow("Enable notifications", settings.notificationsEnabled, vm::setNotificationsEnabled)
            ToggleRow("Reminder sound", settings.reminderSoundEnabled, vm::setReminderSoundEnabled)
            ToggleRow("Daily summary", settings.dailySummaryEnabled, vm::setDailySummaryEnabled)
        }
        SettingsSection("Privacy") {
            ToggleRow("Share anonymous usage analytics", settings.analyticsEnabled, vm::setAnalyticsEnabled)
            ToggleRow("Crash reporting", settings.crashReportingEnabled, vm::setCrashReportingEnabled)
        }
        SettingsSection("Help") {
            Text(
                "MyDo stores everything locally on this device. For questions or feedback, use the thumbs-down button in chat to reach the team.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MydoSpacing.small),
            )
        }
        SettingsSection("About") {
            Text("MyDo", style = MaterialTheme.typography.bodyLarge)
            Text("A local-first task manager.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = MydoSpacing.medium, bottom = MydoSpacing.small))
    Column { content() }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MydoSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> EnumRow(label: String, current: T, options: List<T>, display: (T) -> String, onSelect: (T) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }.padding(vertical = MydoSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(display(current), color = MaterialTheme.colorScheme.primary)
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(option); showDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == current, onClick = { onSelect(option); showDialog = false })
                            Text(display(option))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } },
        )
    }
}
