package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Edit: ImageVector
    get() {
        if (_Edit != null) {
            return _Edit!!
        }
        _Edit = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(16f)
                    verticalLineToRelative(16f)
                    horizontalLineToRelative(-16f)
                    close()
                }
            ) {
                path(
                    stroke = SolidColor(Color(0xFF02609B)),
                    strokeLineWidth = 1.3325f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(11.333f, 2f)
                    curveTo(11.508f, 1.825f, 11.716f, 1.686f, 11.945f, 1.591f)
                    curveTo(12.174f, 1.497f, 12.419f, 1.448f, 12.667f, 1.448f)
                    curveTo(12.914f, 1.448f, 13.16f, 1.497f, 13.388f, 1.591f)
                    curveTo(13.617f, 1.686f, 13.825f, 1.825f, 14f, 2f)
                    curveTo(14.175f, 2.175f, 14.314f, 2.383f, 14.409f, 2.612f)
                    curveTo(14.503f, 2.841f, 14.552f, 3.086f, 14.552f, 3.333f)
                    curveTo(14.552f, 3.581f, 14.503f, 3.826f, 14.409f, 4.055f)
                    curveTo(14.314f, 4.284f, 14.175f, 4.492f, 14f, 4.667f)
                    lineTo(5f, 13.667f)
                    lineTo(1.333f, 14.667f)
                    lineTo(2.333f, 11f)
                    lineTo(11.333f, 2f)
                    close()
                }
            }
        }.build()

        return _Edit!!
    }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null
