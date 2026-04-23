package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Home: ImageVector
    get() {
        if (_HomeSvg != null) {
            return _HomeSvg!!
        }
        _HomeSvg = ImageVector.Builder(
            name = "HomeSvg",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color(0xFF02609B))) {
                moveTo(8f, 3.707f)
                lineTo(3.333f, 7.441f)
                verticalLineTo(12.667f)
                horizontalLineTo(6.667f)
                verticalLineTo(10f)
                horizontalLineTo(9.333f)
                verticalLineTo(12.667f)
                horizontalLineTo(12.667f)
                verticalLineTo(7.761f)
                curveTo(12.667f, 7.661f, 12.644f, 7.563f, 12.601f, 7.473f)
                curveTo(12.558f, 7.382f, 12.495f, 7.303f, 12.417f, 7.241f)
                lineTo(8f, 3.707f)
                close()
                moveTo(8f, 2f)
                lineTo(13.249f, 6.2f)
                curveTo(13.483f, 6.387f, 13.672f, 6.625f, 13.802f, 6.895f)
                curveTo(13.932f, 7.165f, 14f, 7.461f, 14f, 7.761f)
                verticalLineTo(12.667f)
                curveTo(14f, 13.02f, 13.859f, 13.359f, 13.609f, 13.609f)
                curveTo(13.359f, 13.859f, 13.02f, 14f, 12.667f, 14f)
                horizontalLineTo(3.333f)
                curveTo(2.98f, 14f, 2.641f, 13.859f, 2.391f, 13.609f)
                curveTo(2.14f, 13.359f, 2f, 13.02f, 2f, 12.667f)
                verticalLineTo(7.441f)
                curveTo(2f, 7.241f, 2.045f, 7.044f, 2.132f, 6.863f)
                curveTo(2.218f, 6.683f, 2.344f, 6.525f, 2.5f, 6.4f)
                lineTo(8f, 2f)
                close()
            }
        }.build()

        return _HomeSvg!!
    }

@Suppress("ObjectPropertyName")
private var _HomeSvg: ImageVector? = null
