package com.mydo.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.SearchResult
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun SearchScreen(viewModel: SearchViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; viewModel.onQueryChange(it) },
            placeholder = { Text("Search tasks, projects, labels\u2026") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(MydoSpacing.screenMargin),
        )

        when (val state = uiState) {
            is SearchUiState.Idle -> RecentSearchesList(
                recent = state.recentSearches,
                onSelect = { text = it; viewModel.onQueryChange(it) },
                onRemove = viewModel::removeRecentSearch,
                onClearAll = viewModel::clearRecentSearches,
            )
            SearchUiState.Searching -> Unit
            is SearchUiState.Error -> MydoErrorState(title = "Search failed", message = state.message, modifier = Modifier.fillMaxSize())
            is SearchUiState.Results -> {
                if (state.results.isEmpty) {
                    MydoEmptyState(title = "No results", message = "Try a different search term.", modifier = Modifier.fillMaxSize())
                } else {
                    SearchResultsList(state.results.let { r -> r.tasks + r.projects + r.sections + r.labels + r.filters }, navController)
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesList(
    recent: List<com.mydo.app.domain.model.RecentSearch>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (recent.isEmpty()) return
    LazyColumn(contentPadding = PaddingValues(horizontal = MydoSpacing.screenMargin)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent searches", style = MaterialTheme.typography.labelLarge)
                Text("Clear all", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onClearAll))
            }
        }
        items(recent, key = { it.id }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(entry.query) }.padding(vertical = MydoSpacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(entry.query)
                Text("\u2715", modifier = Modifier.clickable { onRemove(entry.query) })
            }
        }
    }
}

@Composable
private fun SearchResultsList(items: List<SearchResult>, navController: NavController) {
    LazyColumn(contentPadding = PaddingValues(vertical = MydoSpacing.small)) {
        items(items, key = { resultKey(it) }) { result ->
            SearchResultRow(result, navController)
            Divider()
        }
    }
}

private fun resultKey(result: SearchResult): String = when (result) {
    is SearchResult.TaskResult -> "task-${result.task.id}"
    is SearchResult.ProjectResult -> "project-${result.project.id}"
    is SearchResult.SectionResult -> "section-${result.section.id}"
    is SearchResult.LabelResult -> "label-${result.label.id}"
    is SearchResult.FilterResult -> "filter-${result.filter.id}"
}

@Composable
private fun SearchResultRow(result: SearchResult, navController: NavController) {
    when (result) {
        is SearchResult.TaskResult -> MydoTaskRow(
            title = result.task.title,
            completed = result.task.completed,
            priority = result.task.priority,
            metadata = result.task.projectPath,
            onClick = { navController.navigate("taskDetail/${result.task.id}") },
            onCompletionToggle = {},
            modifier = Modifier.padding(horizontal = MydoSpacing.screenMargin),
        )
        is SearchResult.ProjectResult -> SimpleResultRow(
            title = result.project.name,
            subtitle = "Project \u00b7 ${result.taskCount} active",
            onClick = { navController.navigate(Screen.Projects.route) },
        )
        is SearchResult.SectionResult -> SimpleResultRow(
            title = result.section.name,
            subtitle = "Section in ${result.projectName}",
            onClick = { navController.navigate(Screen.Projects.route) },
        )
        is SearchResult.LabelResult -> SimpleResultRow(
            title = result.label.name,
            subtitle = "Label \u00b7 ${result.taskCount} tasks",
            onClick = { navController.navigate(Screen.LabelDetail.createRoute(result.label.id.toString())) },
        )
        is SearchResult.FilterResult -> SimpleResultRow(
            title = result.filter.name,
            subtitle = "Saved filter",
            onClick = { navController.navigate(Screen.FilterResults.createRoute(result.filter.id.toString())) },
        )
    }
}

@Composable
private fun SimpleResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
