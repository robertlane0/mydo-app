package com.mydo.app.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun FilterResultsScreen(viewModel: FilterResultsViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        FilterResultsUiState.Loading -> MydoLoadingState(message = "Running filter\u2026", modifier = Modifier.fillMaxSize())
        is FilterResultsUiState.Error -> MydoErrorState(title = "Unable to run filter", message = state.message, modifier = Modifier.fillMaxSize())
        is FilterResultsUiState.Ready -> {
            Column(modifier = Modifier.fillMaxSize().padding(MydoSpacing.screenMargin)) {
                Text(state.filter.name, style = MaterialTheme.typography.titleLarge)
                Text(state.filter.query, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.tasks.isEmpty()) {
                    MydoEmptyState(title = "No matching tasks", message = "Nothing matches this filter right now.", modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = MydoSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                    ) {
                        items(state.tasks, key = { it.id }) { task ->
                            MydoTaskRow(
                                title = task.title,
                                completed = task.completed,
                                priority = task.priority,
                                metadata = task.projectPath,
                                onClick = { navController.navigate("taskDetail/${task.id}") },
                                onCompletionToggle = { },
                            )
                        }
                    }
                }
            }
        }
    }
}
