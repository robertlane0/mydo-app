package com.mydo.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.theme.MydoSpacing
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = SearchUiState.Loading,
    )
    
    var focused by remember { mutableStateOf(true) }

    when (val state = uiState) {
        is SearchUiState.Loading -> MydoLoadingState(
            message = "Loading search…",
            modifier = Modifier.fillMaxSize()
        )

        is SearchUiState.Error -> MydoErrorState(
            title = "Search failed",
            message = state.message,
            modifier = Modifier.fillMaxSize()
        )

        is SearchUiState.Ready -> {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Search field
                SearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onClear = { viewModel.clearQuery() },
                    focused = focused,
                )
                
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                
                // Results or empty state
                if (state.query.isNotBlank()) {
                    if (state.tasks.isEmpty()) {
                        MydoEmptyState(
                            title = "No results for \"${state.query}\"",
                            message = "Try a different keyword or check spelling.",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(MydoSpacing.screenMargin),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(MydoSpacing.screenMargin),
                            contentPadding = PaddingValues(vertical = MydoSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                        ) {
                            items(state.tasks, key = { it.id.toString() }) { task ->
                                MydoTaskRow(
                                    title = task.title,
                                    metadata = task.projectPath,
                                    priority = task.priority,
                                    completed = task.completed,
                                    onClick = { navController.navigate("taskDetail/${task.id}") },
                                    onCompletionToggle = { },
                                )
                            }
                        }
                    }
                } else {
                    // Show recent searches / suggestions
                    SearchSuggestions(onSuggestionClick = { query ->
                        viewModel.onQueryChange(query)
                    })
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focused: Boolean,
) {
    var text by remember { mutableStateOf(query) }
    
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onQueryChange(newText)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(MydoSpacing.screenMargin),
        placeholder = { Text("Search tasks, projects, labels…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (text.isNotBlank()) {
IconButton(onClick = { 
                    text = ""
                    onClear()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Search,
        ),
        visualTransformation = VisualTransformation.None,
        singleLine = true,
        isError = false,
    )
}

@Composable
fun SearchSuggestions(
    onSuggestionClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MydoSpacing.screenMargin),
    ) {
        Text(
            text = "Recent searches",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MydoSpacing.small),
        )
        
        // TODO: Load from preferences
        val recentSearches = remember { listOf("project report", "meeting notes", "groceries") }
        
        if (recentSearches.isEmpty()) {
            Text(
                text = "No recent searches",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = MydoSpacing.medium),
            )
        } else {
            LazyColumn {
                items(recentSearches) { query ->
                    androidx.compose.material3.ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MydoSpacing.extraSmall),
                        leadingContent = { Icon(Icons.Default.History, contentDescription = "History") },
                        headlineContent = { Text(query) },
                    )
                }
            }
        }
    }
}