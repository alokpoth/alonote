package com.noto.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel

/**
 * State managing drag-to-reorder for LazyColumn.
 */
class ReorderableState(
    val lazyListState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    private val scrollChannel = Channel<Float>(Channel.CONFLATED)

    suspend fun handleAutoScroll() {
        for (delta in scrollChannel) {
            lazyListState.scrollBy(delta)
        }
    }

    fun onDragStart(index: Int) {
        draggedIndex = index
        dragOffsetY = 0f
    }

    fun onDrag(dragAmountY: Float) {
        val currentIndex = draggedIndex ?: return
        dragOffsetY += dragAmountY

        val layoutInfo = lazyListState.layoutInfo
        val currentItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex } ?: return
        val currentItemCenter = currentItem.offset + dragOffsetY + currentItem.size / 2f

        // Check if dragged over any other visible item
        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != currentIndex &&
                currentItemCenter in item.offset.toFloat()..(item.offset + item.size).toFloat()
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            onMove(currentIndex, targetIndex)
            // Adjust drag offset to prevent visual jitter
            dragOffsetY -= (targetItem.offset - currentItem.offset)
            draggedIndex = targetIndex
        }

        // Auto-scroll when near boundaries
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset
        val threshold = 72f
        if (currentItem.offset + dragOffsetY < viewportStart + threshold) {
            scrollChannel.trySend(-20f)
        } else if (currentItem.offset + currentItem.size + dragOffsetY > viewportEnd - threshold) {
            scrollChannel.trySend(20f)
        }
    }

    fun onDragEnd() {
        draggedIndex = null
        dragOffsetY = 0f
    }
}

@Composable
fun rememberReorderableState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableState {
    val state = remember(lazyListState, onMove) {
        ReorderableState(lazyListState, onMove)
    }
    LaunchedEffect(state) {
        state.handleAutoScroll()
    }
    return state
}

/**
 * Modifier applied to items to enable drag-and-drop reordering.
 */
@Composable
fun Modifier.reorderableItem(
    state: ReorderableState,
    index: Int
): Modifier {
    val isDragging = state.draggedIndex == index
    val elevation by animateFloatAsState(targetValue = if (isDragging) 8f else 0f, label = "elevation")
    val scale by animateFloatAsState(targetValue = if (isDragging) 1.03f else 1f, label = "scale")

    return this
        .zIndex(if (isDragging) 2f else 1f)
        .graphicsLayer {
            translationY = if (isDragging) state.dragOffsetY else 0f
            scaleX = scale
            scaleY = scale
        }
        .shadow(elevation.dp)
        .pointerInput(index) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.onDragStart(index) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y)
                },
                onDragEnd = { state.onDragEnd() },
                onDragCancel = { state.onDragEnd() }
            )
        }
}
