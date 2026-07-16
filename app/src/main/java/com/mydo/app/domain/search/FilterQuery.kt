package com.mydo.app.domain.search

import com.mydo.app.domain.model.Priority

/**
 * Parsed representation of the query language shared by Search (specs08-search.md) and
 * Saved Filters (specs14-filters.md): bare words AND together, `field:value` narrows by
 * attribute, commas within a field OR its values, a leading `-` negates a term, and
 * parentheses group sub-expressions.
 */
sealed interface FilterQuery {
    data class And(val terms: List<FilterQuery>) : FilterQuery
    data class Or(val terms: List<FilterQuery>) : FilterQuery
    data class Not(val term: FilterQuery) : FilterQuery

    /** Bare word: matches task title or description, case-insensitively. */
    data class Text(val value: String) : FilterQuery

    data class ProjectIs(val name: String) : FilterQuery
    data class SectionIs(val name: String) : FilterQuery
    data class LabelIs(val name: String) : FilterQuery
    data class PriorityIs(val priority: Priority) : FilterQuery
    data class DueOn(val date: DateSpec) : FilterQuery
    data class DueBefore(val date: DateSpec) : FilterQuery
    data class DueAfter(val date: DateSpec) : FilterQuery
    data class DueBetween(val start: DateSpec, val endInclusive: DateSpec) : FilterQuery
    data class NoDue(val value: Boolean) : FilterQuery
    data class CreatedOn(val date: DateSpec) : FilterQuery
    data class CreatedBetween(val start: DateSpec, val endInclusive: DateSpec) : FilterQuery
    data class CompletedIs(val value: Boolean) : FilterQuery
    data class RecurringIs(val value: Boolean) : FilterQuery
    data class HasAttachment(val value: Boolean) : FilterQuery
    data class HasSubtasks(val value: Boolean) : FilterQuery
    data class HasProject(val value: Boolean) : FilterQuery
    data class InInbox(val value: Boolean) : FilterQuery

    /** The empty query: matches everything. */
    data object MatchAll : FilterQuery
}

/** A date value from the query language: relative keyword or an absolute calendar date. */
sealed interface DateSpec {
    data class Absolute(val year: Int, val month: Int, val day: Int) : DateSpec
    data object Today : DateSpec
    data object Tomorrow : DateSpec
    data object Yesterday : DateSpec
    data object StartOfWeek : DateSpec
    data object EndOfWeek : DateSpec
    data object StartOfMonth : DateSpec
    data object EndOfMonth : DateSpec
}

class FilterQueryParseException(message: String) : Exception(message)
