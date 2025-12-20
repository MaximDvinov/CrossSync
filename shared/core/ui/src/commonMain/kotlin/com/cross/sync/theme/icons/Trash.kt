package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Trash: ImageVector
    get() {
        if (_Trash != null) {
            return _Trash!!
        }
        _Trash = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF875454)),
                strokeLineWidth = 1.3325f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 4f)
                horizontalLineTo(14f)
                moveTo(12.667f, 4f)
                verticalLineTo(13.333f)
                curveTo(12.667f, 13.687f, 12.526f, 14.026f, 12.276f, 14.276f)
                curveTo(12.026f, 14.526f, 11.687f, 14.667f, 11.333f, 14.667f)
                horizontalLineTo(4.667f)
                curveTo(4.313f, 14.667f, 3.974f, 14.526f, 3.724f, 14.276f)
                curveTo(3.474f, 14.026f, 3.333f, 13.687f, 3.333f, 13.333f)
                verticalLineTo(4f)
                moveTo(5.333f, 4f)
                verticalLineTo(2.667f)
                curveTo(5.333f, 2.313f, 5.474f, 1.974f, 5.724f, 1.724f)
                curveTo(5.974f, 1.474f, 6.313f, 1.333f, 6.667f, 1.333f)
                horizontalLineTo(9.333f)
                curveTo(9.687f, 1.333f, 10.026f, 1.474f, 10.276f, 1.724f)
                curveTo(10.526f, 1.974f, 10.667f, 2.313f, 10.667f, 2.667f)
                verticalLineTo(4f)
                moveTo(6.667f, 7.333f)
                verticalLineTo(11.333f)
                moveTo(9.333f, 7.333f)
                verticalLineTo(11.333f)
            }
        }.build()

        return _Trash!!
    }

@Suppress("ObjectPropertyName")
private var _Trash: ImageVector? = null
