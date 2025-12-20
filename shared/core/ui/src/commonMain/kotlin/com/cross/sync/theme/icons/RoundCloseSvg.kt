package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Close: ImageVector
    get() {
        if (_RoundCloseSvg != null) {
            return _RoundCloseSvg!!
        }
        _RoundCloseSvg = ImageVector.Builder(
            name = "RoundCloseSvg",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color(0xFF02609B))) {
                moveTo(12.2f, 3.807f)
                curveTo(12.138f, 3.745f, 12.065f, 3.696f, 11.984f, 3.662f)
                curveTo(11.904f, 3.629f, 11.817f, 3.612f, 11.73f, 3.612f)
                curveTo(11.643f, 3.612f, 11.556f, 3.629f, 11.476f, 3.662f)
                curveTo(11.395f, 3.696f, 11.322f, 3.745f, 11.26f, 3.807f)
                lineTo(8f, 7.06f)
                lineTo(4.74f, 3.8f)
                curveTo(4.678f, 3.738f, 4.605f, 3.689f, 4.524f, 3.656f)
                curveTo(4.444f, 3.623f, 4.357f, 3.605f, 4.27f, 3.605f)
                curveTo(4.183f, 3.605f, 4.096f, 3.623f, 4.016f, 3.656f)
                curveTo(3.935f, 3.689f, 3.862f, 3.738f, 3.8f, 3.8f)
                curveTo(3.738f, 3.862f, 3.689f, 3.935f, 3.656f, 4.016f)
                curveTo(3.623f, 4.096f, 3.605f, 4.183f, 3.605f, 4.27f)
                curveTo(3.605f, 4.357f, 3.623f, 4.444f, 3.656f, 4.524f)
                curveTo(3.689f, 4.605f, 3.738f, 4.678f, 3.8f, 4.74f)
                lineTo(7.06f, 8f)
                lineTo(3.8f, 11.26f)
                curveTo(3.738f, 11.322f, 3.689f, 11.395f, 3.656f, 11.476f)
                curveTo(3.623f, 11.556f, 3.605f, 11.643f, 3.605f, 11.73f)
                curveTo(3.605f, 11.817f, 3.623f, 11.904f, 3.656f, 11.984f)
                curveTo(3.689f, 12.065f, 3.738f, 12.138f, 3.8f, 12.2f)
                curveTo(3.862f, 12.262f, 3.935f, 12.311f, 4.016f, 12.344f)
                curveTo(4.096f, 12.377f, 4.183f, 12.395f, 4.27f, 12.395f)
                curveTo(4.357f, 12.395f, 4.444f, 12.377f, 4.524f, 12.344f)
                curveTo(4.605f, 12.311f, 4.678f, 12.262f, 4.74f, 12.2f)
                lineTo(8f, 8.94f)
                lineTo(11.26f, 12.2f)
                curveTo(11.322f, 12.262f, 11.395f, 12.311f, 11.476f, 12.344f)
                curveTo(11.556f, 12.377f, 11.643f, 12.395f, 11.73f, 12.395f)
                curveTo(11.817f, 12.395f, 11.904f, 12.377f, 11.984f, 12.344f)
                curveTo(12.065f, 12.311f, 12.138f, 12.262f, 12.2f, 12.2f)
                curveTo(12.262f, 12.138f, 12.311f, 12.065f, 12.344f, 11.984f)
                curveTo(12.377f, 11.904f, 12.395f, 11.817f, 12.395f, 11.73f)
                curveTo(12.395f, 11.643f, 12.377f, 11.556f, 12.344f, 11.476f)
                curveTo(12.311f, 11.395f, 12.262f, 11.322f, 12.2f, 11.26f)
                lineTo(8.94f, 8f)
                lineTo(12.2f, 4.74f)
                curveTo(12.453f, 4.487f, 12.453f, 4.06f, 12.2f, 3.807f)
                close()
            }
        }.build()

        return _RoundCloseSvg!!
    }

@Suppress("ObjectPropertyName")
private var _RoundCloseSvg: ImageVector? = null
