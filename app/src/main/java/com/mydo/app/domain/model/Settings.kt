package com.mydo.app.domain.model

enum class DefaultHomeView { INBOX, TODAY, UPCOMING }
enum class StartOfWeek { SUNDAY, MONDAY }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Local preferences (specs10-settings.md). Everything here is stored in the
 * key-value `preferences` table and takes effect immediately when changed — there is no
 * account or server to sync to. Data import/export (the spec's "Data" section) lands in
 * a later step; it isn't part of this set.
 */
data class AppSettings(
    // General
    val defaultHomeView: DefaultHomeView = DefaultHomeView.TODAY,
    val defaultPriority: Priority = Priority.P4,
    val defaultReminderMinutesBefore: Int = 30,
    val startOfWeek: StartOfWeek = StartOfWeek.MONDAY,

    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val compactTaskRows: Boolean = false,

    // Productivity
    val dailyGoal: Int = 0,
    val showCompletedTasks: Boolean = true,
    val showWeekends: Boolean = true,
    val completionAnimationEnabled: Boolean = true,

    // Notifications
    val notificationsEnabled: Boolean = true,
    val reminderSoundEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = false,

    // Privacy
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true,
)

/** Preference-table keys backing [AppSettings]; centralized so the mapping only lives in one place. */
object SettingsKeys {
    const val DEFAULT_HOME_VIEW = "settings.general.default_home_view"
    const val DEFAULT_PRIORITY = "settings.general.default_priority"
    const val DEFAULT_REMINDER_MINUTES = "settings.general.default_reminder_minutes"
    const val START_OF_WEEK = "settings.general.start_of_week"

    const val THEME_MODE = "settings.appearance.theme_mode"
    const val USE_DYNAMIC_COLOR = "settings.appearance.dynamic_color"
    const val COMPACT_TASK_ROWS = "settings.appearance.compact_rows"

    const val DAILY_GOAL = "settings.productivity.daily_goal"
    const val SHOW_COMPLETED_TASKS = "settings.productivity.show_completed"
    const val SHOW_WEEKENDS = "settings.productivity.show_weekends"
    const val COMPLETION_ANIMATION = "settings.productivity.completion_animation"

    const val NOTIFICATIONS_ENABLED = "settings.notifications.enabled"
    const val REMINDER_SOUND_ENABLED = "settings.notifications.reminder_sound"
    const val DAILY_SUMMARY_ENABLED = "settings.notifications.daily_summary"

    const val ANALYTICS_ENABLED = "settings.privacy.analytics"
    const val CRASH_REPORTING_ENABLED = "settings.privacy.crash_reporting"
}

fun Map<String, String>.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    fun bool(key: String, default: Boolean) = this[key]?.toBooleanStrictOrNull() ?: default
    fun int(key: String, default: Int) = this[key]?.toIntOrNull() ?: default
    return AppSettings(
        defaultHomeView = this[SettingsKeys.DEFAULT_HOME_VIEW]?.let { runCatching { DefaultHomeView.valueOf(it) }.getOrNull() } ?: defaults.defaultHomeView,
        defaultPriority = this[SettingsKeys.DEFAULT_PRIORITY]?.let { runCatching { Priority.valueOf(it) }.getOrNull() } ?: defaults.defaultPriority,
        defaultReminderMinutesBefore = int(SettingsKeys.DEFAULT_REMINDER_MINUTES, defaults.defaultReminderMinutesBefore),
        startOfWeek = this[SettingsKeys.START_OF_WEEK]?.let { runCatching { StartOfWeek.valueOf(it) }.getOrNull() } ?: defaults.startOfWeek,
        themeMode = this[SettingsKeys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
        useDynamicColor = bool(SettingsKeys.USE_DYNAMIC_COLOR, defaults.useDynamicColor),
        compactTaskRows = bool(SettingsKeys.COMPACT_TASK_ROWS, defaults.compactTaskRows),
        dailyGoal = int(SettingsKeys.DAILY_GOAL, defaults.dailyGoal),
        showCompletedTasks = bool(SettingsKeys.SHOW_COMPLETED_TASKS, defaults.showCompletedTasks),
        showWeekends = bool(SettingsKeys.SHOW_WEEKENDS, defaults.showWeekends),
        completionAnimationEnabled = bool(SettingsKeys.COMPLETION_ANIMATION, defaults.completionAnimationEnabled),
        notificationsEnabled = bool(SettingsKeys.NOTIFICATIONS_ENABLED, defaults.notificationsEnabled),
        reminderSoundEnabled = bool(SettingsKeys.REMINDER_SOUND_ENABLED, defaults.reminderSoundEnabled),
        dailySummaryEnabled = bool(SettingsKeys.DAILY_SUMMARY_ENABLED, defaults.dailySummaryEnabled),
        analyticsEnabled = bool(SettingsKeys.ANALYTICS_ENABLED, defaults.analyticsEnabled),
        crashReportingEnabled = bool(SettingsKeys.CRASH_REPORTING_ENABLED, defaults.crashReportingEnabled),
    )
}
