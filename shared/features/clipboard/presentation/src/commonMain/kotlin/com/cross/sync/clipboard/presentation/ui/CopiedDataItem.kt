package com.cross.sync.clipboard.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import coil3.compose.AsyncImage
import com.cross.sync.clipboard.presentation.CopiedDataStable
import com.cross.sync.theme.AppTheme

@Suppress("NonSkippableComposable")
@Composable
fun CopiedDataItem(
    modifier: Modifier = Modifier,
    copiedData: CopiedDataStable,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    when (copiedData) {
        is CopiedDataStable.Text -> TextCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick
        )

        is CopiedDataStable.FormattedText -> FormatedCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick
        )

        is CopiedDataStable.Image -> ImageCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick
        )

        is CopiedDataStable.File -> FilesCopiedDataItem(
            modifier = modifier,
            copiedData = copiedData,
            isCurrent = isSelected,
            onClick = onClick
        )
    }
}

@Composable
fun ImageCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.Image,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Box(
        modifier = Modifier.clickable(onClick = onClick).then(selectableModifier)
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
    ) {
        AsyncImage(
            model = copiedData.imagePath,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium
        )
    }
}

@Suppress("NonSkippableComposable")
@Composable
fun TextCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.Text,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {

    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Box(
        modifier = Modifier.clickable(onClick = onClick).then(selectableModifier)
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = copiedData.text,
            style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Suppress("NonSkippableComposable")
@Composable
fun FormatedCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.FormattedText,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Box(
        modifier = Modifier.clickable(onClick = onClick).then(selectableModifier)
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = htmlToAnnotatedString(
                copiedData.text,
                style = HtmlStyle(isTextColorEnabled = true, indentUnit = 16.sp)
            ),
            style = AppTheme.typography.regular14.copy(color = AppTheme.colors.onSurface),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Suppress("NonSkippableComposable")
@Composable
fun FilesCopiedDataItem(
    modifier: Modifier,
    copiedData: CopiedDataStable.File,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {

    val selectableModifier = if (isCurrent) {
        modifier.border(2.dp, AppTheme.colors.primary, AppTheme.shapes.round10)
    } else {
        modifier
    }
    Column(
        modifier = Modifier.clickable(onClick = onClick).then(selectableModifier)
            .clip(AppTheme.shapes.round10)
            .background(AppTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        copiedData.filePaths.forEach {
            BasicText(
                text = it,
                style = AppTheme.typography.regular16.copy(color = AppTheme.colors.onSurface),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}