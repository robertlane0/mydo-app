package com.mydo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Composable
fun MydoLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MydoSpacing.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            modifier = Modifier.padding(top = MydoSpacing.medium),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun MydoEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(MydoSpacing.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = message,
            modifier = Modifier.padding(top = MydoSpacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .padding(top = MydoSpacing.large)
                    .fillMaxWidth(),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun MydoErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(MydoSpacing.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = message,
            modifier = Modifier.padding(top = MydoSpacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = MydoSpacing.large),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

/** A single app-wide snackbar request, with an optional action (typically "Undo"). */
data class MydoSnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/**
 * Process-wide snackbar bus. Screens each still own local [SnackbarHostState]s for
 * events that originate inside their own Scaffold (Inbox, etc.), but some feedback
 * (most notably the globally-available Task Composer bottom sheet in [com.mydo.app.ui.app.MydoApp])
 * has no single owning screen to show a snackbar from. Those emit here instead, and the
 * single [MydoSnackbarHost] hosted by [com.mydo.app.ui.app.MydoApp]'s Scaffold displays them.
 */
object MydoSnackbarController {
    private val _messages = MutableSharedFlow<MydoSnackbarMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<MydoSnackbarMessage> = _messages.asSharedFlow()

    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(MydoSnackbarMessage(message, actionLabel, onAction))
    }
}

@Composable
fun MydoSnackbarHost(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    LaunchedEffect(snackbarHostState) {
        MydoSnackbarController.messages.collect { request ->
            val result = snackbarHostState.showSnackbar(
                message = request.message,
                actionLabel = request.actionLabel,
                withDismissAction = request.actionLabel == null,
            )
            if (result == SnackbarResult.ActionPerformed) request.onAction?.invoke()
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}
