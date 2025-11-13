package com.cross.sync.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.AppTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    containerColor: Color? = AppTheme.colors.surfaceVariant,
    containerShape: Shape = AppTheme.shapes.round10,
    paddingContent: PaddingValues = PaddingValues(0.dp, 8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
    containerShadow: androidx.compose.ui.graphics.shadow.Shadow = AppTheme.shadows.default,
    anchor: @Composable (Modifier) -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backgroundModifier = if (containerColor != null) {
        Modifier.background(containerColor, containerShape)
    } else {
        Modifier
    }
    AppPopup(
        visible = expanded,
        offset = IntOffset(0, 10),
        onDismissRequest = onDismissRequest,
        anchor = anchor,
    ) {
        Column(
            modifier = Modifier
                .dropShadow(shape = containerShape, shadow = containerShadow)
                .then(backgroundModifier)
                .padding(paddingContent),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    containerColor: Color = AppTheme.colors.surfaceVariant,
    containerShape: RoundedCornerShape = AppTheme.shapes.round10,
    paddingContent: PaddingValues = PaddingValues(0.dp, 0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
    containerShadow: androidx.compose.ui.graphics.shadow.Shadow = AppTheme.shadows.default,
    anchor: @Composable (Modifier) -> Unit,
    onDismissRequest: () -> Unit,
    items: DropdownItems,
) {
    AppDropdownMenu(
        expanded = expanded,
        containerColor = null,
        containerShape = containerShape,
        paddingContent = paddingContent,
        verticalArrangement = verticalArrangement,
        containerShadow = containerShadow,
        anchor = anchor,
        onDismissRequest = onDismissRequest,
    ) {
        items.list.forEachIndexed { index, item ->
            val containerShape = when {
                items.list.size == 1 -> containerShape
                index == 0 -> {
                    containerShape.copy(
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    )
                }

                index < items.list.lastIndex -> {
                    containerShape.copy(all = CornerSize(4.dp))
                }

                else -> {
                    containerShape.copy(topStart = CornerSize(4.dp), topEnd = CornerSize(2.dp))
                }
            }

            var isVisible by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(expanded) {
                delay(100L * index)
                isVisible = expanded
            }

            AnimatedVisibility(
                isVisible,
                enter = fadeIn() + expandVertically(spring(Spring.DampingRatioMediumBouncy)),
                exit = fadeOut() + shrinkVertically(spring(Spring.DampingRatioMediumBouncy))
            ) {
                BasicText(
                    text = item.text,
                    style = AppTheme.typography.regular14.copy(AppTheme.colors.onSurfaceVariant),
                    modifier = Modifier
                        .clickable(onClick = item.onClick)
                        .background(containerColor, containerShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

class DropdownMenuBuilder {
    val list: MutableList<DropdownItem> = mutableListOf()

    fun build(): DropdownItems = DropdownItems(list.toPersistentList())

    fun append(text: String, onClick: () -> Unit) {
        list.add(DropdownItem(text, onClick))
    }
}


inline fun buildDropdownItems(builder: DropdownMenuBuilder.() -> Unit): DropdownItems =
    DropdownMenuBuilder().apply(builder).build()

@Stable
data class DropdownItem(
    val text: String,
    val onClick: () -> Unit,
)

@Stable
data class DropdownItems(
    val list: PersistentList<DropdownItem>,
)


@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
) = AppDropdownMenuItem(onClick = onClick) {
    BasicText(
        text = text,
        style = AppTheme.typography.regular14.copy(AppTheme.colors.onSurfaceVariant),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun AppDropdownMenuItem(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(content = content)
}