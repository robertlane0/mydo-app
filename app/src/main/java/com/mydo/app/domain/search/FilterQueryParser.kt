package com.mydo.app.domain.search

import com.mydo.app.domain.model.Priority

/**
 * Parses the query language documented in specs14-filters.md ("Query Syntax"), reused by
 * Search (specs08-search.md). Grammar (informally):
 *
 * ```
 * expr       := notTerm*                      // space-separated terms AND together
 * notTerm    := "-" notTerm | primary
 * primary    := "(" expr ")" | fieldTerm | TEXT
 * fieldTerm  := FIELD ":" value ("," value)*   // comma = OR within the field
 *             | "@" value ("," value)*         // shorthand for label:
 *             | "p" DIGIT                      // shorthand for priority:
 * ```
 *
 * A blank query parses to [FilterQuery.MatchAll]. Invalid syntax throws
 * [FilterQueryParseException] with a message suitable for an inline error
 * ("Invalid recurrence rule" / "Invalid query syntax" per spec).
 */
object FilterQueryParser {

    private val PRIORITY_SHORTHAND = Regex("^[pP]([1-4])$")
    private val ABSOLUTE_DATE = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

    // Parsing is pure and deterministic, so the same rule string always yields the same
    // AST; memoizing keeps repeated evaluation (e.g. re-running a saved filter, or typing
    // then deleting a character) cheap. See specs16-recurring-tasks.md's equivalent
    // caching note for RRULE parsing, which this search/filter language mirrors.
    private val cache = LinkedHashMap<String, FilterQuery>()
    private const val CACHE_LIMIT = 64

    fun parse(query: String): FilterQuery {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return FilterQuery.MatchAll
        cache[trimmed]?.let { return it }

        val tokens = tokenize(trimmed)
        val cursor = Cursor(tokens)
        val result = parseExpr(cursor)
        if (!cursor.isAtEnd) {
            throw FilterQueryParseException("Unexpected '${cursor.peek()}'")
        }

        cache[trimmed] = result
        if (cache.size > CACHE_LIMIT) {
            cache.remove(cache.keys.first())
        }
        return result
    }

    /** Validates without throwing; returns a user-facing error message, or null if valid. */
    fun validate(query: String): String? = try {
        parse(query)
        null
    } catch (e: FilterQueryParseException) {
        e.message ?: "Invalid query syntax"
    }

    // -- Tokenizer --

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()
        var inQuotes = false

        fun flush() {
            if (buffer.isNotEmpty()) {
                tokens += buffer.toString()
                buffer.clear()
            }
        }

        for (char in input) {
            when {
                char == '"' -> inQuotes = !inQuotes
                inQuotes -> buffer.append(char)
                char.isWhitespace() -> flush()
                char == '(' || char == ')' -> {
                    flush()
                    tokens += char.toString()
                }
                else -> buffer.append(char)
            }
        }
        flush()
        return tokens
    }

    private class Cursor(private val tokens: List<String>) {
        var index = 0
            private set
        val isAtEnd: Boolean get() = index >= tokens.size
        fun peek(): String? = tokens.getOrNull(index)
        fun advance(): String = tokens[index++]
    }

    // -- Grammar --

    private fun parseExpr(cursor: Cursor): FilterQuery {
        val terms = mutableListOf<FilterQuery>()
        while (!cursor.isAtEnd && cursor.peek() != ")") {
            terms += parseNotTerm(cursor)
        }
        return when (terms.size) {
            0 -> FilterQuery.MatchAll
            1 -> terms[0]
            else -> FilterQuery.And(terms)
        }
    }

    private fun parseNotTerm(cursor: Cursor): FilterQuery {
        val token = cursor.peek() ?: throw FilterQueryParseException("Unexpected end of query")
        if (token == "-") {
            cursor.advance()
            return FilterQuery.Not(parseNotTerm(cursor))
        }
        return parsePrimary(cursor)
    }

    private fun parsePrimary(cursor: Cursor): FilterQuery {
        val token = cursor.advance()
        if (token == "(") {
            val expr = parseExpr(cursor)
            if (cursor.peek() != ")") throw FilterQueryParseException("Missing closing ')'")
            cursor.advance()
            return expr
        }
        if (token.startsWith("-") && token.length > 1) {
            return FilterQuery.Not(parseFieldOrText(token.substring(1)))
        }
        return parseFieldOrText(token)
    }

    private fun parseFieldOrText(token: String): FilterQuery {
        if (token.startsWith("@") && token.length > 1) {
            return orOf(token.substring(1).split(",").map { it.trim() }.filter { it.isNotEmpty() }) { FilterQuery.LabelIs(it) }
        }
        PRIORITY_SHORTHAND.matchEntire(token)?.let {
            return FilterQuery.PriorityIs(priorityFromDigit(it.groupValues[1]))
        }

        val separatorIndex = token.indexOf(':')
        if (separatorIndex <= 0) {
            return FilterQuery.Text(token)
        }

        val field = token.substring(0, separatorIndex).lowercase()
        val rawValue = token.substring(separatorIndex + 1)
        if (rawValue.isEmpty()) throw FilterQueryParseException("Missing value for '$field:'")

        return when (field) {
            "project" -> orOf(splitValues(rawValue)) { FilterQuery.ProjectIs(it) }
            "section" -> orOf(splitValues(rawValue)) { FilterQuery.SectionIs(it) }
            "label" -> orOf(splitValues(rawValue)) { FilterQuery.LabelIs(it) }
            "priority" -> orOf(splitValues(rawValue)) { FilterQuery.PriorityIs(priorityFromDigit(it)) }
            "due" -> parseDueField(rawValue)
            "duebefore" -> FilterQuery.DueBefore(parseDateSpec(rawValue))
            "dueafter" -> FilterQuery.DueAfter(parseDateSpec(rawValue))
            "nodue" -> FilterQuery.NoDue(parseBool(rawValue))
            "completed" -> FilterQuery.CompletedIs(parseBool(rawValue))
            "created" -> parseCreatedField(rawValue)
            "recurring" -> FilterQuery.RecurringIs(parseBool(rawValue))
            "hasattachment" -> FilterQuery.HasAttachment(parseBool(rawValue))
            "hassubtasks" -> FilterQuery.HasSubtasks(parseBool(rawValue))
            "hasproject" -> FilterQuery.HasProject(parseBool(rawValue))
            "inbox" -> FilterQuery.InInbox(parseBool(rawValue))
            else -> throw FilterQueryParseException("Unknown field '$field:'")
        }
    }

    private fun parseDueField(rawValue: String): FilterQuery {
        if (rawValue.equals("overdue", ignoreCase = true)) {
            return FilterQuery.And(listOf(FilterQuery.DueBefore(DateSpec.Today), FilterQuery.CompletedIs(false)))
        }
        if (rawValue.contains("..")) {
            val (start, end) = splitRange(rawValue)
            return FilterQuery.DueBetween(parseDateSpec(start), parseDateSpec(end))
        }
        if (rawValue.equals("week", ignoreCase = true)) {
            return FilterQuery.DueBetween(DateSpec.Today, DateSpec.EndOfWeek)
        }
        if (rawValue.equals("month", ignoreCase = true)) {
            return FilterQuery.DueBetween(DateSpec.Today, DateSpec.EndOfMonth)
        }
        return orOf(splitValues(rawValue)) { FilterQuery.DueOn(parseDateSpec(it)) }
    }

    private fun parseCreatedField(rawValue: String): FilterQuery {
        if (rawValue.contains("..")) {
            val (start, end) = splitRange(rawValue)
            return FilterQuery.CreatedBetween(parseDateSpec(start), parseDateSpec(end))
        }
        return FilterQuery.CreatedOn(parseDateSpec(rawValue))
    }

    private fun splitRange(rawValue: String): Pair<String, String> {
        val parts = rawValue.split("..", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw FilterQueryParseException("Invalid date range '$rawValue'")
        }
        return parts[0].trim() to parts[1].trim()
    }

    private fun splitValues(rawValue: String): List<String> =
        rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { throw FilterQueryParseException("Missing value") }

    private fun orOf(values: List<String>, build: (String) -> FilterQuery): FilterQuery {
        val terms = values.map(build)
        return if (terms.size == 1) terms[0] else FilterQuery.Or(terms)
    }

    private fun priorityFromDigit(digit: String): Priority = when (digit) {
        "1" -> Priority.P1
        "2" -> Priority.P2
        "3" -> Priority.P3
        "4" -> Priority.P4
        else -> throw FilterQueryParseException("Invalid priority '$digit'")
    }

    private fun parseBool(value: String): Boolean = when (value.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> throw FilterQueryParseException("Invalid boolean value '$value'")
    }

    private fun parseDateSpec(value: String): DateSpec {
        when (value.lowercase()) {
            "today" -> return DateSpec.Today
            "tomorrow" -> return DateSpec.Tomorrow
            "yesterday" -> return DateSpec.Yesterday
            "week" -> return DateSpec.EndOfWeek
            "month" -> return DateSpec.EndOfMonth
        }
        val match = ABSOLUTE_DATE.matchEntire(value) ?: throw FilterQueryParseException("Invalid date '$value'")
        val (year, month, day) = match.destructured
        return DateSpec.Absolute(year.toInt(), month.toInt(), day.toInt())
    }
}
