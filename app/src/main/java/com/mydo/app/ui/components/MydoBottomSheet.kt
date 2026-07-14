package com.mydo.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mydo.app.ui.theme.MydoSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MydoBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(
                topStart = MydoSpacing.large,
                topEnd = MydoSpacing.large,
            ),
            scrimColor = Color.Black.copy(alpha = 0.32f),
            content = content,
        )
    }
}
