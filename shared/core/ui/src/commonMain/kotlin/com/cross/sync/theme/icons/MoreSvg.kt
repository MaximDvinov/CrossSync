package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.More: ImageVector
    get() {
        if (_MoreSvg != null) {
            return _MoreSvg!!
        }
        _MoreSvg = ImageVector.Builder(
            name = "MoreSvg",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f
        ).apply {
            path(fill = SolidColor(Color(0xFF02609B))) {
                moveTo(1.953f, 4.656f)
                curveTo(2.318f, 4.656f, 2.63f, 4.786f, 2.891f, 5.047f)
                curveTo(3.151f, 5.307f, 3.281f, 5.625f, 3.281f, 6f)
                curveTo(3.281f, 6.375f, 3.151f, 6.693f, 2.891f, 6.953f)
                curveTo(2.63f, 7.214f, 2.318f, 7.344f, 1.953f, 7.344f)
                curveTo(1.589f, 7.344f, 1.276f, 7.214f, 1.016f, 6.953f)
                curveTo(0.755f, 6.693f, 0.625f, 6.375f, 0.625f, 6f)
                curveTo(0.625f, 5.625f, 0.755f, 5.307f, 1.016f, 5.047f)
                curveTo(1.276f, 4.786f, 1.589f, 4.656f, 1.953f, 4.656f)
                close()
                moveTo(9.953f, 4.656f)
                curveTo(10.318f, 4.656f, 10.63f, 4.786f, 10.891f, 5.047f)
                curveTo(11.151f, 5.307f, 11.281f, 5.625f, 11.281f, 6f)
                curveTo(11.281f, 6.375f, 11.151f, 6.693f, 10.891f, 6.953f)
                curveTo(10.63f, 7.214f, 10.318f, 7.344f, 9.953f, 7.344f)
                curveTo(9.589f, 7.344f, 9.276f, 7.214f, 9.016f, 6.953f)
                curveTo(8.755f, 6.693f, 8.625f, 6.375f, 8.625f, 6f)
                curveTo(8.625f, 5.625f, 8.755f, 5.307f, 9.016f, 5.047f)
                curveTo(9.276f, 4.786f, 9.589f, 4.656f, 9.953f, 4.656f)
                close()
                moveTo(5.953f, 4.656f)
                curveTo(6.318f, 4.656f, 6.63f, 4.786f, 6.891f, 5.047f)
                curveTo(7.151f, 5.307f, 7.281f, 5.625f, 7.281f, 6f)
                curveTo(7.281f, 6.375f, 7.151f, 6.693f, 6.891f, 6.953f)
                curveTo(6.63f, 7.214f, 6.318f, 7.344f, 5.953f, 7.344f)
                curveTo(5.589f, 7.344f, 5.276f, 7.214f, 5.016f, 6.953f)
                curveTo(4.755f, 6.693f, 4.625f, 6.375f, 4.625f, 6f)
                curveTo(4.625f, 5.625f, 4.755f, 5.307f, 5.016f, 5.047f)
                curveTo(5.276f, 4.786f, 5.589f, 4.656f, 5.953f, 4.656f)
                close()
            }
        }.build()

        return _MoreSvg!!
    }

@Suppress("ObjectPropertyName")
private var _MoreSvg: ImageVector? = null
