package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.platform.NotificationScheduler
import java.util.UUID

/**
 * Keeps OS-scheduled reminder alarms in sync with the local `reminders` table (AGENTS.md
 * step 5: "reschedule reminders after task edits, recurrence completion, reboot, and app
 * update"). The database is always the source of truth; this only ever mirrors its current
 * state into [NotificationScheduler], so every method here is safe — and cheap enough — to
 * call redundantly after any task or reminder mutation rather than trying to reason about
 * exactly which edits could have affected scheduling.
 */
class ReminderAlarmCoordinator(
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
    private val timeProvider: TimeProvider,
) {
    /**
     * Re-arms every enabled, future reminder for [taskId] whose task still exists and isn't
     * completed; cancels the rest. Call after creating, editing, or deleting a reminder, and
     * after any edit to the task itself (title changes flow into the posted notification).
     */
    suspend fun sync(taskId: UUID) {
        val task = (taskRepository.getById(taskId) as? AppResult.Success)?.value
        val reminders = (reminderRepository.getByTask(taskId) as? AppResult.Success)?.value.orEmpty()
        val now = timeProvider.nowUtcMillis()
        for (reminder in reminders) {
            val shouldBeArmed = task != null && !task.completed && reminder.enabled && reminder.triggerAtUtcMillis > now
            if (shouldBeArmed) {
                notificationScheduler.schedule(reminder, task!!)
            } else {
                notificationScheduler.cancel(reminder.id)
            }
        }
    }

    /**
     * Cancels a single reminder's alarm directly by id. Used when a reminder has just been
     * deleted and its row — and thus its `taskId` — is no longer available to look up via
     * [sync].
     */
    suspend fun cancelReminder(reminderId: UUID) = notificationScheduler.cancel(reminderId)

    /**
     * Cancels every scheduled alarm for [taskId]'s reminders. Call this *before* deleting a
     * task — the reminders table's `ON DELETE CASCADE` from tasks removes the reminder rows
     * as a side effect of the task delete, so by the time [sync] could read them back they'd
     * already be gone and their alarms would be orphaned rather than cancelled.
     */
    suspend fun cancelAll(taskId: UUID) {
        val reminders = (reminderRepository.getByTask(taskId) as? AppResult.Success)?.value.orEmpty()
        for (reminder in reminders) notificationScheduler.cancel(reminder.id)
    }

    /**
     * Re-arms every enabled, future reminder across every task in the database. Call after
     * boot, after an app update, and once notification permission is freshly granted — the
     * cases where Android drops previously-scheduled exact alarms out from under the app.
     */
    suspend fun rescheduleAll() {
        val now = timeProvider.nowUtcMillis()
        val pending = (reminderRepository.getAllPending(now) as? AppResult.Success)?.value.orEmpty()
        for (reminder in pending) {
            val task = (taskRepository.getById(reminder.taskId) as? AppResult.Success)?.value
            if (task != null && !task.completed) notificationScheduler.schedule(reminder, task)
        }
    }

    /**
     * The ids of every reminder currently eligible to have an armed alarm. Snapshot this
     * *before* a full local-data wipe or import replace, since those operations remove the
     * old reminder rows outright — by the time they're gone there's nothing left to look up
     * a taskId from, so the alarms they left behind would otherwise be uncancellable.
     */
    suspend fun pendingReminderIds(): List<UUID> =
        (reminderRepository.getAllPending(timeProvider.nowUtcMillis()) as? AppResult.Success)?.value.orEmpty().map { it.id }

    /** Cancels a previously-[pendingReminderIds] snapshot's alarms by id. */
    suspend fun cancel(reminderIds: List<UUID>) {
        for (id in reminderIds) notificationScheduler.cancel(id)
    }
}
