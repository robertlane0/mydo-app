package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.domain.search.FilterQueryEvaluator
import com.mydo.app.domain.search.FilterQueryParseException
import com.mydo.app.domain.search.FilterQueryParser
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId
import java.util.UUID

/** specs14-filters.md, "Limits": prevents unbounded growth of the saved-filters list. */
const val MAX_SAVED_FILTERS = 100

class ObserveFiltersUseCase(private val filterRepository: FilterRepository) {
    operator fun invoke(): Flow<AppResult<List<Filter>>> = filterRepository.observeAll()
}

/** Parses without persisting, for live validation as the user types a query (specs14, "Query Builder"). */
class ValidateFilterQueryUseCase {
    operator fun invoke(query: String): String? = FilterQueryParser.validate(query)
}

class CreateFilterUseCase(private val filterRepository: FilterRepository) {
    suspend operator fun invoke(name: String, query: String, favorite: Boolean = false): AppResult<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return AppResult.Failure(ValidationError("blank_name", "Give this filter a name"))
        FilterQueryParser.validate(query)?.let { return AppResult.Failure(ValidationError("invalid_query", it)) }

        when (val existing = filterRepository.findByName(trimmedName)) {
            is AppResult.Failure -> return existing
            is AppResult.Success -> if (existing.value != null) {
                return AppResult.Failure(ValidationError("duplicate_name", "A filter named \"$trimmedName\" already exists"))
            }
        }
        when (val countResult = filterRepository.count()) {
            is AppResult.Failure -> return countResult
            is AppResult.Success -> if (countResult.value >= MAX_SAVED_FILTERS) {
                return AppResult.Failure(ValidationError("limit_reached", "You've reached the limit of $MAX_SAVED_FILTERS saved filters"))
            }
        }
        return filterRepository.create(Filter(id = UUID.randomUUID(), name = trimmedName, query = query.trim(), favorite = favorite))
    }
}

class UpdateFilterUseCase(private val filterRepository: FilterRepository) {
    suspend operator fun invoke(filter: Filter): AppResult<Unit> {
        val trimmedName = filter.name.trim()
        if (trimmedName.isEmpty()) return AppResult.Failure(ValidationError("blank_name", "Give this filter a name"))
        FilterQueryParser.validate(filter.query)?.let { return AppResult.Failure(ValidationError("invalid_query", it)) }

        when (val existing = filterRepository.findByName(trimmedName)) {
            is AppResult.Failure -> return existing
            is AppResult.Success -> if (existing.value != null && existing.value.id != filter.id) {
                return AppResult.Failure(ValidationError("duplicate_name", "A filter named \"$trimmedName\" already exists"))
            }
        }
        return filterRepository.update(filter.copy(name = trimmedName, query = filter.query.trim()))
    }
}

class DeleteFilterUseCase(private val filterRepository: FilterRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = filterRepository.delete(id)
}

class ToggleFilterFavoriteUseCase(private val filterRepository: FilterRepository) {
    suspend operator fun invoke(filter: Filter): AppResult<Unit> = filterRepository.update(filter.copy(favorite = !filter.favorite))
}

/** Evaluates a saved (or draft) filter's query against every task (specs14, "Filter Results"). */
class RunFilterUseCase(
    private val taskRepository: TaskRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(query: String): AppResult<List<TaskSummary>> {
        val parsed = try {
            FilterQueryParser.parse(query)
        } catch (e: FilterQueryParseException) {
            return AppResult.Failure(ValidationError("invalid_query", e.message ?: "Invalid query syntax"))
        }
        val contexts = when (val result = taskRepository.getFilterContexts()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val evaluator = FilterQueryEvaluator(zoneId)
        val matches = contexts.filter { evaluator.matches(parsed, it) }
            .sortedWith(compareBy({ it.completed }, { it.dueAtUtcMillis ?: Long.MAX_VALUE }))
            .map {
                TaskSummary(
                    id = it.taskId,
                    title = it.title,
                    completed = it.completed,
                    priority = it.priority,
                    dueAtUtcMillis = it.dueAtUtcMillis,
                    projectPath = it.projectName,
                    recurring = it.recurring,
                )
            }
        return AppResult.Success(matches)
    }
}
