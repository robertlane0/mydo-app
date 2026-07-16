package com.mydo.app.domain.search

import com.mydo.app.domain.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterQueryParserTest {

    @Test
    fun blankQueryParsesToMatchAll() {
        assertEquals(FilterQuery.MatchAll, FilterQueryParser.parse(""))
        assertEquals(FilterQuery.MatchAll, FilterQueryParser.parse("   "))
    }

    @Test
    fun bareWordsBecomeTextTerms() {
        val result = FilterQueryParser.parse("groceries milk")
        assertTrue(result is FilterQuery.And)
        val terms = (result as FilterQuery.And).terms
        assertEquals(listOf(FilterQuery.Text("groceries"), FilterQuery.Text("milk")), terms)
    }

    @Test
    fun fieldValueParsesToTypedTerm() {
        assertEquals(FilterQuery.ProjectIs("Work"), FilterQueryParser.parse("project:Work"))
        assertEquals(FilterQuery.PriorityIs(Priority.P1), FilterQueryParser.parse("priority:1"))
        assertEquals(FilterQuery.PriorityIs(Priority.P2), FilterQueryParser.parse("p2"))
    }

    @Test
    fun labelShorthandParsesToLabelIs() {
        assertEquals(FilterQuery.LabelIs("urgent"), FilterQueryParser.parse("@urgent"))
    }

    @Test
    fun commaWithinFieldBecomesOr() {
        val result = FilterQueryParser.parse("label:work,personal")
        assertEquals(
            FilterQuery.Or(listOf(FilterQuery.LabelIs("work"), FilterQuery.LabelIs("personal"))),
            result,
        )
    }

    @Test
    fun leadingDashNegatesTerm() {
        val result = FilterQueryParser.parse("-completed:true")
        assertEquals(FilterQuery.Not(FilterQuery.CompletedIs(true)), result)
    }

    @Test
    fun parenthesesGroupSubExpressions() {
        val result = FilterQueryParser.parse("(project:Work project:Personal) label:urgent")
        assertTrue(result is FilterQuery.And)
        val terms = (result as FilterQuery.And).terms
        assertEquals(2, terms.size)
        assertEquals(
            FilterQuery.And(listOf(FilterQuery.ProjectIs("Work"), FilterQuery.ProjectIs("Personal"))),
            terms[0],
        )
        assertEquals(FilterQuery.LabelIs("urgent"), terms[1])
    }

    @Test
    fun dueOverdueExpandsToDueBeforeTodayAndNotCompleted() {
        val result = FilterQueryParser.parse("due:overdue")
        assertEquals(
            FilterQuery.And(listOf(FilterQuery.DueBefore(DateSpec.Today), FilterQuery.CompletedIs(false))),
            result,
        )
    }

    @Test
    fun dueRangeParsesToDueBetween() {
        val result = FilterQueryParser.parse("due:2026-07-01..2026-07-31")
        assertEquals(
            FilterQuery.DueBetween(DateSpec.Absolute(2026, 7, 1), DateSpec.Absolute(2026, 7, 31)),
            result,
        )
    }

    @Test
    fun unknownFieldThrows() {
        assertNotNull(FilterQueryParser.validate("bogus:value"))
    }

    @Test
    fun missingClosingParenThrows() {
        assertNotNull(FilterQueryParser.validate("(project:Work"))
    }

    @Test
    fun validQueryValidatesToNull() {
        assertNull(FilterQueryParser.validate("priority:1 -completed:true"))
    }
}
