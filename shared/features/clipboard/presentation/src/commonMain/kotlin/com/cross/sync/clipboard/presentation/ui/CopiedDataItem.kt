package com.cross.sync.clipboard.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import coil3.compose.AsyncImage
import com.cross.sync.clipboard.presentation.model.CopiedDataStable
import com.cross.sync.components.AppDropdownMenu
import com.cross.sync.components.buildDropdownItems
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.More
import com.cross.sync.utils.dayFormat

@Suppress("NonSkippableComposable")
@Composable
fun CopiedDataItem(
    modifier: Modifier = Modifier,
    copiedData: CopiedDataStable,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isShowTag: Boolean = true,
) {
    when (copiedData) {
        is CopiedDataStable.Text -> TextCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onDeleteClick = onDeleteClick,
            isShowTag = isShowTag
        )

        is CopiedDataStable.FormattedText -> FormatedCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onDeleteClick = onDeleteClick,
            isShowTag = isShowTag

        )

        is CopiedDataStable.Image -> ImageCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onDeleteClick = onDeleteClick,
            isShowTag = isShowTag

        )

        is CopiedDataStable.File -> FilesCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onDeleteClick = onDeleteClick,
            isShowTag = isShowTag
        )
    }
}

@Composable
fun ImageCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.Image,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isShowTag: Boolean,
) {
    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Column {
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                )
                .then(selectableModifier)
                .clip(AppTheme.shapes.round10)
                .background(AppTheme.colors.surface),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = copiedData.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        }

        if (isShowTag) {
            CopiedDataTags(
                Modifier,
                copiedData = copiedData,
                onDeleteClick
            )
        }

    }

}

@Suppress("NonSkippableComposable")
@Composable
fun TextCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.Text,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isShowTag: Boolean,
) {

    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Column {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
                .then(selectableModifier)
                .clip(AppTheme.shapes.round10)
                .background(AppTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            BasicText(
                text = copiedData.text,
                style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        if (isShowTag) {
            CopiedDataTags(
                Modifier,
                copiedData = copiedData,
                onDeleteClick
            )
        }
    }

}

@Suppress("NonSkippableComposable")
@Composable
fun FormatedCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.FormattedText,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isShowTag: Boolean,
) {
    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }

    Column {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                )
                .then(selectableModifier)
                .clip(AppTheme.shapes.round10)
                .background(AppTheme.colors.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            val text = if (copiedData.mimeType == "text/rtf") {
                buildAnnotatedString { append(copiedData.plainText) }
            } else {
                htmlToAnnotatedString(
                    copiedData.text,
                    style = HtmlStyle(isTextColorEnabled = true, indentUnit = 16.sp)
                )
            }
            BasicText(
                text = text,
                style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                inlineContent = mapOf(),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        if (isShowTag) {
            CopiedDataTags(
                Modifier,
                copiedData = copiedData,
                onDeleteClick
            )
        }
    }

}

@Suppress("NonSkippableComposable")
@Composable
fun FilesCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.File,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isShowTag: Boolean,
) {
    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Column {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                )
                .then(selectableModifier)
                .clip(AppTheme.shapes.round10)
                .background(AppTheme.colors.surface)
                .padding(10.dp)
        ) {
            copiedData.filePaths.forEach {
                BasicText(
                    text = it,
                    style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        if (isShowTag) {
            CopiedDataTags(
                Modifier,
                copiedData = copiedData,
                onDeleteClick
            )
        }
    }

}

@Composable
fun CopiedDataTags(
    modifier: Modifier = Modifier,
    copiedData: CopiedDataStable,
    onDeleteClick: () -> Unit,
) {
    var visibleMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.zIndex(10f).fillMaxWidth().padding(top = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
    ) {
        copiedData.application?.let { app ->
            Row(
                modifier = Modifier
                    .background(
                        AppTheme.colors.surfaceVariant,
                        AppTheme.shapes.round50percent
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                app.icon?.let { icon ->
                    AsyncImage(
                        model = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clip(AppTheme.shapes.round3),
                        contentScale = ContentScale.Crop
                    )
                }

                BasicText(
                    text = app.name,
                    style = AppTheme.typography.regular10.copy(color = AppTheme.colors.onSurfaceVariant),
                    modifier = Modifier.widthIn(max = 120.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .background(
                    AppTheme.colors.surfaceVariant,
                    AppTheme.shapes.round50percent
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .height(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = when (copiedData) {
                    is CopiedDataStable.File -> "File"
                    is CopiedDataStable.FormattedText -> copiedData.mimeType
                    is CopiedDataStable.Image -> "Image"
                    is CopiedDataStable.Text -> "Text"
                },
                style = AppTheme.typography.regular10.copy(color = AppTheme.colors.onSurfaceVariant)
            )
        }

        Row(
            modifier = Modifier
                .background(
                    AppTheme.colors.surfaceVariant,
                    AppTheme.shapes.round50percent
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .height(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = copiedData.date.dayFormat(),
                style = AppTheme.typography.regular10.copy(color = AppTheme.colors.onSurfaceVariant)
            )
        }

        AppDropdownMenu(
            expanded = visibleMenu,
            anchor = { anchorModifier ->
                Row(
                    modifier = anchorModifier
                        .clickable(onClick = { visibleMenu = true })
                        .background(
                            AppTheme.colors.surfaceVariant,
                            AppTheme.shapes.round50percent
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppTheme.icons.More,
                        contentDescription = null,
                        tint = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            },
            onDismissRequest = { visibleMenu = false },
            items = buildDropdownItems {
                append("Delete") {
                    onDeleteClick()
                }
            }
        )

    }
}

