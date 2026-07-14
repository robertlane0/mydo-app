package com.mydo.app.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoSnackbarHost
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.home.HomeUiState
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MydoApp(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = "MyDo") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Text(text = "+")
            }
        },
        snackbarHost = { MydoSnackbarHost() },
    ) { paddingValues ->
        when (val state = uiState) {
            HomeUiState.Loading -> MydoLoadingState(
                message = "Opening local database…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            is HomeUiState.Error -> MydoErrorState(
                title = "Unable to open local data.",
                message = state.message,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            is HomeUiState.Ready -> {
                if (state.tasks.isEmpty()) {
                    MydoEmptyState(
                        title = "No tasks yet",
                        message = "Add a task to start capturing local work.",
                        actionLabel = "Add a task",
                        onAction = { },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(MydoSpacing.screenMargin),
                        verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                    ) {
                        items(state.tasks, key = { it.id }) { task ->
                            MydoTaskRow(
                                title = task.title,
                                metadata = task.projectPath,
                                priority = task.priority,
                                completed = task.completed,
                                onClick = { },
                                onCompletionToggle = { },
                            )
                        }
                    }
                }
            }
        }
    }
}
