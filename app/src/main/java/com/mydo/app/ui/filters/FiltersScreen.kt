package com.mydo.app.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.theme.MydoSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    viewModel: FiltersViewModel,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is FiltersUiState.Loading -> MydoLoadingState(
            message = "Loading filters…",
            modifier = Modifier.fillMaxSize()
        )

        is FiltersUiState.Error -> MydoErrorState(
            title = "Unable to load filters",
            message = state.message,
            modifier = Modifier.fillMaxSize()
        )

        is FiltersUiState.Ready -> {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top app bar
                TopAppBar(
                    title = { Text("Filters") },
                    actions = {
                        IconButton(onClick = { /* TODO: Show create filter dialog */ }) {
                            Icon(Icons.Default.Add, contentDescription = "Add filter")
                        }
                    },
                )

                // Filters list
                if (state.filters.isEmpty()) {
                    MydoEmptyState(
                        title = "No Filters Yet",
                        message = "Create filters to quickly find tasks with complex queries.",
                        actionLabel = "Create Filter",
                        onAction = { /* TODO: Show create filter dialog */ },
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
                        items(state.filters, key = { it.id }) { filter ->
                            FilterRow(
                                filter = filter,
                                onClick = { /* TODO: Open filter results */ },
                                onEdit = { /* TODO: Show edit dialog */ },
                                onDelete = { /* TODO: Show delete confirmation */ },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRow(
    filter: com.mydo.app.domain.model.Filter,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MydoSpacing.extraSmall),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MydoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Star icon for favorite
            if (filter.favorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite filter",
                    tint = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(end = MydoSpacing.small))
            }
            
            // Filter name
            Text(
                text = filter.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            // Actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit filter")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete filter")
            }
        }
    }
}