package com.mydo.app.domain.recurrence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceCalculatorTest {

    @Test
    fun dailyAdvancesByInterval() {
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, interval = 3)
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2026, 1, 4), next)
    }

    @Test
    fun weeklyWithoutByDayAdvancesOneWeek() {
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY)
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 5)) // Monday
        assertEquals(LocalDate.of(2026, 1, 12), next)
    }

    @Test
    fun weeklyMultiDayPicksNextDayInSameWeek() {
        // Mon/Wed/Fri; after Monday -> Wednesday same week.
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val monday = LocalDate.of(2026, 1, 5)
        val next = RecurrenceCalculator.nextOccurrence(rule, monday)
        assertEquals(LocalDate.of(2026, 1, 7), next) // Wednesday
    }

    @Test
    fun weeklyMultiDayWrapsToNextWeekAfterLastDay() {
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val friday = LocalDate.of(2026, 1, 9)
        val next = RecurrenceCalculator.nextOccurrence(rule, friday)
        assertEquals(LocalDate.of(2026, 1, 12), next) // following Monday
    }

    @Test
    fun everyWeekdaySkipsWeekend() {
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = RecurrenceRule.WEEKDAYS)
        val friday = LocalDate.of(2026, 1, 9)
        val next = RecurrenceCalculator.nextOccurrence(rule, friday)
        assertEquals(LocalDate.of(2026, 1, 12), next) // Monday, not Saturday
    }

    @Test
    fun biweeklySingleDayJumpsTwoWeeks() {
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, interval = 2, byDay = setOf(DayOfWeek.TUESDAY))
        val tuesday = LocalDate.of(2026, 1, 6)
        val next = RecurrenceCalculator.nextOccurrence(rule, tuesday)
        assertEquals(LocalDate.of(2026, 1, 20), next)
    }

    @Test
    fun monthlyOnDayAdvancesToNextMonth() {
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, byMonthDay = setOf(15))
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 15))
        assertEquals(LocalDate.of(2026, 2, 15), next)
    }

    @Test
    fun monthlyLastDayClampsInShortMonths() {
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, byMonthDay = setOf(31))
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 28), next) // Feb 2026 has 28 days
    }

    @Test
    fun monthlyNegativeOneMeansLastDayOfMonth() {
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, byMonthDay = setOf(-1))
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun yearlyAdvancesToNextYear() {
        val rule = RecurrenceRule(RecurrenceFrequency.YEARLY, byMonth = setOf(1), byMonthDay = setOf(1))
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2027, 1, 1), next)
    }

    @Test
    fun leapYearFeb29HandledForYearlyRecurrence() {
        // 2028 is a leap year; 2029 is not, so Feb 29 clamps to Feb 28.
        val rule = RecurrenceRule(RecurrenceFrequency.YEARLY, byMonth = setOf(2), byMonthDay = setOf(29))
        val next = RecurrenceCalculator.nextOccurrence(rule, LocalDate.of(2028, 2, 29))
        assertEquals(LocalDate.of(2029, 2, 28), next)
    }

    @Test
    fun canGenerateNextRespectsCount() {
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, count = 3)
        assertTrue(RecurrenceCalculator.canGenerateNext(rule, completedOccurrenceNumber = 2, nextDate = LocalDate.of(2026, 1, 3)))
        assertFalse(RecurrenceCalculator.canGenerateNext(rule, completedOccurrenceNumber = 3, nextDate = LocalDate.of(2026, 1, 4)))
    }

    @Test
    fun canGenerateNextRespectsUntil() {
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, until = LocalDate.of(2026, 1, 5))
        assertTrue(RecurrenceCalculator.canGenerateNext(rule, 1, LocalDate.of(2026, 1, 5)))
        assertFalse(RecurrenceCalculator.canGenerateNext(rule, 1, LocalDate.of(2026, 1, 6)))
    }

    @Test
    fun ruleRoundTripsThroughStringParsing() {
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, interval = 2, byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), count = 10)
        val parsed = RecurrenceRuleParser.parse(rule.toRuleString())
        assertEquals(rule, parsed)
    }

    @Test
    fun invalidRuleStringThrows() {
        try {
            RecurrenceRuleParser.parse("FREQ=NOTAFREQ")
            throw AssertionError("Expected RecurrenceRuleException")
        } catch (e: RecurrenceRuleException) {
            assertEquals("Invalid recurrence rule", e.message)
        }
    }

    @Test
    fun validateReturnsNullForGoodRule() {
        assertNull(RecurrenceRuleParser.validate("FREQ=DAILY;INTERVAL=2"))
    }

    @Test
    fun summaryCoversCommonPresets() {
        assertEquals("Every day", RecurrenceSummaryFormatter.summarize(RecurrenceRule(RecurrenceFrequency.DAILY)))
        assertEquals("Every weekday", RecurrenceSummaryFormatter.summarize(RecurrenceRule(RecurrenceFrequency.WEEKLY, byDay = RecurrenceRule.WEEKDAYS)))
        assertEquals(
            "Monthly on the last day",
            RecurrenceSummaryFormatter.summarize(RecurrenceRule(RecurrenceFrequency.MONTHLY, byMonthDay = setOf(-1))),
        )
    }
}
