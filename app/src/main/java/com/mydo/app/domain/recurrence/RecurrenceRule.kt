package com.mydo.app.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/**
 * A parsed `recurringRule` value: the RFC 5545 RRULE subset defined in
 * specs16-recurring-tasks.md ("Recurrence Rule Format").
 */
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    /** Only meaningful for [RecurrenceFrequency.WEEKLY]. Empty means "same weekday as the anchor". */
    val byDay: Set<DayOfWeek> = emptySet(),
    /** Only meaningful for [RecurrenceFrequency.MONTHLY]/[RecurrenceFrequency.YEARLY]. -1 means the last day of the month. */
    val byMonthDay: Set<Int> = emptySet(),
    /** Only meaningful for [RecurrenceFrequency.YEARLY]. 1-12. */
    val byMonth: Set<Int> = emptySet(),
    val count: Int? = null,
    val until: LocalDate? = null,
) {
    fun toRuleString(): String = buildString {
        append("FREQ=").append(frequency.name)
        if (interval != 1) append(";INTERVAL=").append(interval)
        if (byDay.isNotEmpty()) {
            append(";BYDAY=").append(byDay.sortedBy { it.value }.joinToString(",") { it.toRuleCode() })
        }
        if (byMonthDay.isNotEmpty()) {
            append(";BYMONTHDAY=").append(byMonthDay.sorted().joinToString(","))
        }
        if (byMonth.isNotEmpty()) {
            append(";BYMONTH=").append(byMonth.sorted().joinToString(","))
        }
        if (count != null) append(";COUNT=").append(count)
        if (until != null) append(";UNTIL=").append(until.toCompactString())
    }

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
    }
}

class RecurrenceRuleException(message: String) : Exception(message)

/**
 * Parses/serializes recurrence rule strings. Parsing is cached per unique rule string
 * (specs16: "RRULE parsing cached per rule string") since the same rule is re-parsed on
 * every render of a recurring row and on every completion.
 */
object RecurrenceRuleParser {
    private val cache = LinkedHashMap<String, RecurrenceRule>()
    private const val CACHE_LIMIT = 128

    fun parse(raw: String): RecurrenceRule {
        cache[raw]?.let { return it }
        val rule = parseInternal(raw)
        cache[raw] = rule
        if (cache.size > CACHE_LIMIT) cache.remove(cache.keys.first())
        return rule
    }

    /** Validates without throwing; returns a user-facing error, or null if valid. */
    fun validate(raw: String): String? = try {
        parse(raw)
        null
    } catch (e: RecurrenceRuleException) {
        e.message ?: "Invalid recurrence rule"
    }

    private fun parseInternal(raw: String): RecurrenceRule {
        if (raw.isBlank()) throw RecurrenceRuleException("Invalid recurrence rule")
        val parts = raw.split(";").filter { it.isNotBlank() }
        val fields = mutableMapOf<String, String>()
        for (part in parts) {
            val eq = part.indexOf('=')
            if (eq <= 0) throw RecurrenceRuleException("Invalid recurrence rule")
            fields[part.substring(0, eq).trim().uppercase()] = part.substring(eq + 1).trim()
        }

        val freq = try {
            RecurrenceFrequency.valueOf(fields["FREQ"] ?: throw RecurrenceRuleException("Invalid recurrence rule"))
        } catch (e: IllegalArgumentException) {
            throw RecurrenceRuleException("Invalid recurrence rule")
        }

        val interval = fields["INTERVAL"]?.toIntOrNull()?.takeIf { it > 0 } ?: run {
            if (fields.containsKey("INTERVAL")) throw RecurrenceRuleException("Invalid recurrence rule") else 1
        }

        val byDay = fields["BYDAY"]?.split(",")?.map { it.trim().toDayOfWeekOrThrow() }?.toSet() ?: emptySet()
        val byMonthDay = fields["BYMONTHDAY"]?.split(",")?.map {
            it.trim().toIntOrNull()?.takeIf { d -> d in -1..31 && d != 0 } ?: throw RecurrenceRuleException("Invalid recurrence rule")
        }?.toSet() ?: emptySet()
        val byMonth = fields["BYMONTH"]?.split(",")?.map {
            it.trim().toIntOrNull()?.takeIf { m -> m in 1..12 } ?: throw RecurrenceRuleException("Invalid recurrence rule")
        }?.toSet() ?: emptySet()
        val count = fields["COUNT"]?.toIntOrNull()?.takeIf { it > 0 }
            ?: fields["COUNT"]?.let { throw RecurrenceRuleException("Invalid recurrence rule") }
        val until = fields["UNTIL"]?.toLocalDateOrThrow()

        if (freq == RecurrenceFrequency.WEEKLY && byDay.isEmpty() && fields.containsKey("BYDAY")) {
            throw RecurrenceRuleException("Invalid recurrence rule")
        }

        return RecurrenceRule(
            frequency = freq,
            interval = interval,
            byDay = byDay,
            byMonthDay = byMonthDay,
            byMonth = byMonth,
            count = count,
            until = until,
        )
    }

    private fun String.toDayOfWeekOrThrow(): DayOfWeek = when (this.uppercase()) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> throw RecurrenceRuleException("Invalid recurrence rule")
    }

    private fun String.toLocalDateOrThrow(): LocalDate {
        if (this.length != 8) throw RecurrenceRuleException("Invalid recurrence rule")
        return try {
            LocalDate.of(this.substring(0, 4).toInt(), this.substring(4, 6).toInt(), this.substring(6, 8).toInt())
        } catch (e: Exception) {
            throw RecurrenceRuleException("Invalid recurrence rule")
        }
    }
}

private fun DayOfWeek.toRuleCode(): String = when (this) {
    DayOfWeek.MONDAY -> "MO"
    DayOfWeek.TUESDAY -> "TU"
    DayOfWeek.WEDNESDAY -> "WE"
    DayOfWeek.THURSDAY -> "TH"
    DayOfWeek.FRIDAY -> "FR"
    DayOfWeek.SATURDAY -> "SA"
    DayOfWeek.SUNDAY -> "SU"
}

private fun LocalDate.toCompactString(): String =
    "%04d%02d%02d".format(year, monthValue, dayOfMonth)
