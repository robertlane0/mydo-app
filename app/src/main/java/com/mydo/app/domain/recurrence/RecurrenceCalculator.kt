package com.mydo.app.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Computes the next occurrence date after a given date for a [RecurrenceRule], per
 * specs16-recurring-tasks.md ("Next Due Date Calculation"). Pure date math — callers
 * combine it with `dueAtUtcMillis`'s time-of-day and the local timezone (see
 * specs16 "Timezone": "Recurrence calculated in user's local timezone").
 */
object RecurrenceCalculator {

    /** The next date strictly after [after] that matches [rule]. Never returns [after] itself. */
    fun nextOccurrence(rule: RecurrenceRule, after: LocalDate): LocalDate = when (rule.frequency) {
        RecurrenceFrequency.DAILY -> after.plusDays(rule.interval.toLong())
        RecurrenceFrequency.WEEKLY -> nextWeekly(rule, after)
        RecurrenceFrequency.MONTHLY -> nextMonthly(rule, after)
        RecurrenceFrequency.YEARLY -> nextYearly(rule, after)
    }

    /**
     * Whether a next occurrence should be generated after completing the occurrence
     * numbered [completedOccurrenceNumber] (1-based) whose next computed date is
     * [nextDate]. False once COUNT is reached or UNTIL is passed (specs16, "Business
     * Rules").
     */
    fun canGenerateNext(rule: RecurrenceRule, completedOccurrenceNumber: Int, nextDate: LocalDate): Boolean {
        if (rule.count != null && completedOccurrenceNumber >= rule.count) return false
        if (rule.until != null && nextDate.isAfter(rule.until)) return false
        return true
    }

    private fun nextWeekly(rule: RecurrenceRule, after: LocalDate): LocalDate {
        val days = (rule.byDay.ifEmpty { setOf(after.dayOfWeek) }).map { it.value }.sorted()
        val afterDow = after.dayOfWeek.value
        val sameWeek = days.filter { it > afterDow }
        if (sameWeek.isNotEmpty()) {
            return after.plusDays((sameWeek.first() - afterDow).toLong())
        }
        val mondayOfAfterWeek = after.minusDays((afterDow - 1).toLong())
        val targetMonday = mondayOfAfterWeek.plusWeeks(rule.interval.toLong())
        return targetMonday.plusDays((days.first() - 1).toLong())
    }

    private fun resolveMonthDay(day: Int, month: YearMonth): Int {
        val length = month.lengthOfMonth()
        return when {
            day == -1 -> length
            day > length -> length
            day < -1 -> maxOf(1, length + day + 1)
            else -> day
        }
    }

    private fun nextMonthly(rule: RecurrenceRule, after: LocalDate): LocalDate {
        val dayValues = rule.byMonthDay.ifEmpty { setOf(after.dayOfMonth) }.toList()
        val curMonth = YearMonth.from(after)
        val sameMonthCandidates = dayValues.map { resolveMonthDay(it, curMonth) }.filter { it > after.dayOfMonth }.sorted()
        if (sameMonthCandidates.isNotEmpty()) {
            return curMonth.atDay(sameMonthCandidates.first())
        }
        val targetMonth = curMonth.plusMonths(rule.interval.toLong())
        val resolved = dayValues.map { resolveMonthDay(it, targetMonth) }.sorted()
        return targetMonth.atDay(resolved.first())
    }

    private fun nextYearly(rule: RecurrenceRule, after: LocalDate): LocalDate {
        val months = (rule.byMonth.ifEmpty { setOf(after.monthValue) }).sorted()
        val dayValues = rule.byMonthDay.ifEmpty { setOf(after.dayOfMonth) }

        fun candidateFor(year: Int, month: Int): LocalDate {
            val ym = YearMonth.of(year, month)
            val day = dayValues.minOf { resolveMonthDay(it, ym) }
            return ym.atDay(day)
        }

        val laterMonthsThisYear = months.filter { candidateFor(after.year, it).isAfter(after) }
        if (laterMonthsThisYear.isNotEmpty()) {
            return candidateFor(after.year, laterMonthsThisYear.first())
        }
        return candidateFor(after.year + rule.interval, months.first())
    }
}

/**
 * Generates the short, human-readable summary shown on task rows and in the recurrence
 * editor (specs16-recurring-tasks.md, "Recurrence Display": e.g. "Every weekday",
 * "Monthly on 1st", "Every 2 weeks").
 */
object RecurrenceSummaryFormatter {
    private val cache = LinkedHashMap<String, String>()
    private const val CACHE_LIMIT = 128

    fun summarize(rawRule: String): String {
        cache[rawRule]?.let { return it }
        val summary = try {
            summarize(RecurrenceRuleParser.parse(rawRule))
        } catch (e: RecurrenceRuleException) {
            "Recurring"
        }
        cache[rawRule] = summary
        if (cache.size > CACHE_LIMIT) cache.remove(cache.keys.first())
        return summary
    }

    fun summarize(rule: RecurrenceRule): String = when (rule.frequency) {
        RecurrenceFrequency.DAILY ->
            if (rule.interval == 1) "Every day" else "Every ${rule.interval} days"

        RecurrenceFrequency.WEEKLY -> when {
            rule.byDay.isEmpty() && rule.interval == 1 -> "Every week"
            rule.byDay.isEmpty() -> "Every ${rule.interval} weeks"
            rule.byDay == RecurrenceRule.WEEKDAYS && rule.interval == 1 -> "Every weekday"
            rule.interval == 1 -> "Every ${dayList(rule.byDay)}"
            else -> "Every ${rule.interval} weeks on ${dayList(rule.byDay)}"
        }

        RecurrenceFrequency.MONTHLY -> {
            val dayLabel = rule.byMonthDay.minOrNull()?.let { ordinalOrLast(it) } ?: "the same day"
            if (rule.interval == 1) "Monthly on $dayLabel" else "Every ${rule.interval} months on $dayLabel"
        }

        RecurrenceFrequency.YEARLY -> {
            val month = rule.byMonth.firstOrNull()?.let { monthName(it) }
            val day = rule.byMonthDay.firstOrNull()
            when {
                month != null && day != null -> "Every $month ${ordinalOrLast(day)}"
                rule.interval == 1 -> "Every year"
                else -> "Every ${rule.interval} years"
            }
        }
    }

    private fun dayList(days: Set<DayOfWeek>): String =
        days.sortedBy { it.value }.joinToString(", ") { it.shortLabel() }

    private fun monthName(month: Int): String =
        Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())

    private fun ordinalOrLast(day: Int): String = when {
        day == -1 -> "the last day"
        day % 100 in 11..13 -> "${day}th"
        day % 10 == 1 -> "${day}st"
        day % 10 == 2 -> "${day}nd"
        day % 10 == 3 -> "${day}rd"
        else -> "${day}th"
    }
}

private fun DayOfWeek.shortLabel(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())
