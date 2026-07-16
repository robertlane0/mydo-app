package com.mydo.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydo.app.domain.recurrence.RecurrenceFrequency
import com.mydo.app.domain.recurrence.RecurrenceRule
import java.time.DayOfWeek

/**
 * Preset + custom recurrence editor (specs16-recurring-tasks.md, "Recurrence Editor").
 * Presets cover the common cases; "Custom" exposes frequency/interval/weekday controls
 * directly. Monthly/yearly BYMONTHDAY/BYMONTH stay at whatever the current due date
 * implies, which matches "Monthly on the 1st" style presets without extra UI.
 */
@Composable
fun RecurrenceEditorSheet(
    initialRuleString: String?,
    dueDayOfWeek: DayOfWeek,
    dueDayOfMonth: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var customFrequency by remember { mutableStateOf(RecurrenceFrequency.WEEKLY) }
    var customInterval by remember { mutableIntStateOf(1) }
    var selectedDays by remember { mutableStateOf(setOf(dueDayOfWeek)) }
    var showCustom by remember { mutableStateOf(false) }

    MydoBottomSheet(visible = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Repeat", style = MaterialTheme.typography.titleMedium)

            if (!showCustom) {
                PresetRow("Doesn't repeat") { onSave(""); }
                PresetRow("Every day") { onSave(RecurrenceRule(RecurrenceFrequency.DAILY).toRuleString()) }
                PresetRow("Every weekday (Mon\u2013Fri)") {
                    onSave(RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = RecurrenceRule.WEEKDAYS).toRuleString())
                }
                PresetRow("Every week") {
                    onSave(RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = setOf(dueDayOfWeek)).toRuleString())
                }
                PresetRow("Every month") {
                    onSave(RecurrenceRule(RecurrenceFrequency.MONTHLY, byMonthDay = setOf(dueDayOfMonth)).toRuleString())
                }
                PresetRow("Every year") { onSave(RecurrenceRule(RecurrenceFrequency.YEARLY).toRuleString()) }
                PresetRow("Custom\u2026") { showCustom = true }
                if (initialRuleString != null) {
                    TextButton(onClick = onRemove) { Text("Remove recurrence", color = MaterialTheme.colorScheme.error) }
                }
            } else {
                Text("Repeat every", modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customInterval.toString(),
                        onValueChange = { customInterval = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                        modifier = Modifier.padding(end = 8.dp),
                        singleLine = true,
                    )
                    FrequencyChip("Day(s)", customFrequency == RecurrenceFrequency.DAILY) { customFrequency = RecurrenceFrequency.DAILY }
                    FrequencyChip("Week(s)", customFrequency == RecurrenceFrequency.WEEKLY) { customFrequency = RecurrenceFrequency.WEEKLY }
                    FrequencyChip("Month(s)", customFrequency == RecurrenceFrequency.MONTHLY) { customFrequency = RecurrenceFrequency.MONTHLY }
                }
                if (customFrequency == RecurrenceFrequency.WEEKLY) {
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        (1..7).forEach { dayValue ->
                            val day = DayOfWeek.of(dayValue)
                            val checked = selectedDays.contains(day)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 4.dp)) {
                                Text(dayOfWeekShort(dayValue).take(1))
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedDays = if (isChecked) selectedDays + day else selectedDays - day
                                    },
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = { showCustom = false }) { Text("Back") }
                    TextButton(onClick = {
                        val rule = RecurrenceRule(
                            frequency = customFrequency,
                            interval = customInterval,
                            byDay = if (customFrequency == RecurrenceFrequency.WEEKLY) selectedDays else emptySet(),
                            byMonthDay = if (customFrequency == RecurrenceFrequency.MONTHLY) setOf(dueDayOfMonth) else emptySet(),
                        )
                        onSave(rule.toRuleString())
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun PresetRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun FrequencyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
