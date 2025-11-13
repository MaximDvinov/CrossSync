package com.cross.sync.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun AppPopup(
    visible: Boolean,
    offset: IntOffset = IntOffset(0, 10),
    anchor: @Composable (Modifier) -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    Box {
        anchor(
            Modifier.onGloballyPositioned { coords ->
                val position = coords.localToWindow(Offset.Zero)
                anchorBounds = Rect(
                    position.x,
                    position.y,
                    position.x + coords.size.width,
                    position.y + coords.size.height
                )
            }
        )

        if (visible && anchorBounds != null) {
            anchorBounds?.let {
                Popup(
                    offset = IntOffset(0, it.height.roundToInt()) + offset,
                    onDismissRequest = onDismissRequest,
                    properties = PopupProperties(focusable = true),
                    alignment = Alignment.TopEnd
                ) {
                    content()
                }
            }

        }
    }
}