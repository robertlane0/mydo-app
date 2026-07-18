package com.mydo.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.NotificationManagerCompat
import com.mydo.app.BuildConfig
import com.mydo.app.domain.model.AppSettings
import com.mydo.app.domain.model.DefaultHomeView
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.StartOfWeek
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.theme.MydoSpacing
import java.text.DateFormat
import java.util.Date

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
            NotificationPermissionNotice()
        }
        SettingsSection("Data") {
            DataSection(vm)
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

/**
 * Export/import/clear (specs10-settings.md, "Data"). Export and import both go through the
 * system document chooser — MyDo never picks a location or a source on its own — so the
 * actual read/write only happens once Android hands back a `content://` Uri.
 *
 * Only Replace is offered for import; MyDo does not yet implement merge import, so there's
 * no conflict-resolution step to show.
 */
@Composable
private fun DataSection(vm: SettingsViewModel) {
    val dataState by vm.dataState.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }

    val createExportDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) vm.writeExport(uri) else vm.dismissDataState()
    }
    val openImportDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.onImportFileChosen(uri) else vm.dismissDataState()
    }

    // Once the JSON is built, immediately hand off to the system save chooser for a
    // destination — this state only exists to bridge use-case result -> picker launch.
    LaunchedEffect(dataState) {
        val ready = dataState as? DataOperationState.ExportReady ?: return@LaunchedEffect
        createExportDocument.launch(ready.suggestedFilename)
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { vm.startExport() }.padding(vertical = MydoSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Export local database")
        Text("Save backup", color = MaterialTheme.colorScheme.primary)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { openImportDocument.launch(arrayOf("application/json")) }.padding(vertical = MydoSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Import local database")
        Text("Choose file", color = MaterialTheme.colorScheme.primary)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { showClearConfirm = true }.padding(vertical = MydoSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Clear local data")
        Text("Delete everything", color = MaterialTheme.colorScheme.error)
    }

    when (val state = dataState) {
        DataOperationState.InProgress -> Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = MydoSpacing.small),
            horizontalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        is DataOperationState.ImportPreview -> AlertDialog(
            onDismissRequest = vm::dismissDataState,
            title = { Text("Replace all local data?") },
            text = {
                val exportedOn = remember(state.manifest.exportedAtUtcMillis) {
                    DateFormat.getDateInstance().format(Date(state.manifest.exportedAtUtcMillis))
                }
                Column {
                    Text("This backup was exported on $exportedOn from MyDo ${state.manifest.appVersionName}.")
                    Text(
                        "It contains ${state.manifest.counts.tasks} tasks, ${state.manifest.counts.projects} projects, " +
                            "${state.manifest.counts.labels} labels, and ${state.manifest.counts.filters} filters.",
                        modifier = Modifier.padding(top = MydoSpacing.small),
                    )
                    Text(
                        "Replacing will permanently delete everything currently on this device and replace it with this backup. " +
                            "A safety copy of your current data is saved on this device first when possible.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = MydoSpacing.small),
                    )
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmReplaceImport) { Text("Replace") } },
            dismissButton = { TextButton(onClick = vm::dismissDataState) { Text("Cancel") } },
        )

        is DataOperationState.Message -> Snackbar(
            modifier = Modifier.fillMaxWidth().padding(vertical = MydoSpacing.small),
            action = { TextButton(onClick = vm::dismissDataState) { Text("Dismiss") } },
        ) { Text(state.text) }

        DataOperationState.Idle, is DataOperationState.ExportReady -> Unit
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all local data?") },
            text = { Text("This permanently deletes every task, project, and setting on this device. Consider exporting a backup first — this can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showClearConfirm = false; vm.clearLocalData() }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } },
        )
    }
}

/**
 * If device-level notification permission is denied, reminders silently never fire — explain
 * why and link to the system setting to fix it (specs09-notifications.md, "States and
 * Errors": "If local notifications cannot be scheduled because device permission is denied,
 * explain how to enable permission in system settings").
 */
@Composable
private fun NotificationPermissionNotice() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Recheck on resume — this is the only reliable signal that the user may have
            // just come back from the system settings screen this notice links to.
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!enabled) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = MydoSpacing.small)) {
            Text(
                "Notifications are turned off for MyDo at the device level, so reminders won't appear.",
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                "Open system settings to enable them",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }.padding(top = MydoSpacing.extraSmall),
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
