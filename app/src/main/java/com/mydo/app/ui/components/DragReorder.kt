package com.mydo.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Drives a manual drag-reorder within a single [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]
 * (specs18-drag-reorder.md, "Reorder Mechanics"). Position is tracked by index rather
 * than by hit-testing pointer coordinates against the list's global offsets, since the
 * handle that starts the drag already knows its own index at composition time.
 *
 * [onMove] should reorder the backing list immediately (optimistic, so the UI keeps up
 * with the finger); the caller persists the final order once the drag ends.
 */
class DragDropListState(
    private val lazyListState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    fun offsetForIndex(index: Int): Float = if (index == draggingItemIndex) draggingItemOffset else 0f

    fun onDragStart(index: Int) {
        draggingItemIndex = index
        draggingItemOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        draggingItemOffset += deltaY
        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middle = (startOffset + endOffset) / 2f

        val target = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != draggingItem.index && middle.toInt() in item.offset..(item.offset + item.size)
        }
        if (target != null) {
            onMove(draggingItem.index, target.index)
            draggingItemOffset += (draggingItem.offset - target.offset).toFloat()
            draggingItemIndex = target.index
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffset = 0f
    }
}

@Composable
fun rememberDragDropListState(lazyListState: LazyListState, onMove: (from: Int, to: Int) -> Unit): DragDropListState =
    remember(lazyListState) { DragDropListState(lazyListState, onMove) }

/** Modifier applied to a row so it visually follows the finger while it's the one being dragged. */
fun Modifier.dragReorderOffset(state: DragDropListState, index: Int): Modifier = graphicsLayer {
    translationY = state.offsetForIndex(index)
    alpha = if (state.draggingItemIndex == index) 0.9f else 1f
}

/** A small drag handle glyph; only this handle initiates a drag, so the rest of the row still taps normally. */
@Composable
fun DragHandle(state: DragDropListState, index: Int, onDragEnd: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "\u2261", // ≡ — trigram/"hamburger" glyph, used as a drag handle since this project has no icon library
        style = MaterialTheme.typography.titleMedium,
        color = LocalContentColor.current.copy(alpha = if (state.draggingItemIndex == index) 1f else 0.5f),
        modifier = modifier
            .semantics { contentDescription = "Drag to reorder" }
            .pointerInput(index) {
                detectDragGestures(
                    onDragStart = { state.onDragStart(index) },
                    onDrag = { change, dragAmount -> change.consume(); state.onDrag(dragAmount.y) },
                    onDragEnd = { state.onDragEnd(); onDragEnd() },
                    onDragCancel = { state.onDragEnd() },
                )
            },
    )
}
