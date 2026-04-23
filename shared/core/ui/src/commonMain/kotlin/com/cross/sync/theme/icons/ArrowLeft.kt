package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.ArrowLeft: ImageVector
    get() {
        if (_ArrowLeft != null) {
            return _ArrowLeft!!
        }

        _ArrowLeft = ImageVector.Builder(
            name = "ArrowLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF02609B))) {
                moveTo(10.83f, 12f)
                lineTo(14.71f, 15.88f)
                curveTo(15.1f, 16.27f, 15.1f, 16.9f, 14.71f, 17.29f)
                curveTo(14.32f, 17.68f, 13.69f, 17.68f, 13.3f, 17.29f)
                lineTo(8f, 12f)
                lineTo(13.3f, 6.71f)
                curveTo(13.69f, 6.32f, 14.32f, 6.32f, 14.71f, 6.71f)
                curveTo(15.1f, 7.1f, 15.1f, 7.73f, 14.71f, 8.12f)
                lineTo(10.83f, 12f)
                close()
            }
        }.build()

        return _ArrowLeft!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowLeft: ImageVector? = null
