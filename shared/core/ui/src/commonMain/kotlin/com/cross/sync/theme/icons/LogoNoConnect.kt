package com.cross.sync.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.LogoNoConnect: ImageVector
    get() {
        if (_LogoNoConnect != null) {
            return _LogoNoConnect!!
        }
        _LogoNoConnect = ImageVector.Builder(
            name = "LogoNoConnect",
            defaultWidth = 500.dp,
            defaultHeight = 500.dp,
            viewportWidth = 500f,
            viewportHeight = 500f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF00253C)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(114.03f, 139f)
                curveTo(143.5f, 109.55f, 183.47f, 93f, 225.15f, 93f)
                horizontalLineTo(228.21f)
                curveTo(235.11f, 93f, 239.77f, 100.05f, 237.05f, 106.39f)
                lineTo(219.39f, 147.69f)
                lineTo(206.41f, 178.52f)
                curveTo(203.16f, 186.25f, 192.26f, 186.41f, 188.78f, 178.78f)
                lineTo(183.57f, 167.32f)
                curveTo(181.32f, 162.37f, 175.37f, 160.24f, 170.76f, 163.12f)
                curveTo(164.23f, 167.2f, 158.14f, 172.03f, 152.61f, 177.56f)
                curveTo(133.37f, 196.78f, 122.56f, 222.86f, 122.56f, 250.05f)
                curveTo(122.56f, 277.24f, 133.37f, 303.32f, 152.61f, 322.55f)
                curveTo(171.85f, 341.77f, 197.94f, 352.57f, 225.15f, 352.57f)
                verticalLineTo(393.81f)
                lineTo(222.1f, 400.93f)
                curveTo(220.54f, 404.59f, 216.88f, 406.93f, 212.91f, 406.63f)
                curveTo(206.49f, 406.12f, 200.14f, 405.23f, 193.88f, 403.96f)
                curveTo(163.85f, 397.87f, 136.01f, 383.08f, 114.03f, 361.1f)
                curveTo(84.56f, 331.65f, 68f, 291.7f, 68f, 250.05f)
                curveTo(68f, 208.4f, 84.56f, 168.45f, 114.03f, 139f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF00253C)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(225.15f, 393.81f)
                verticalLineTo(352.57f)
                horizontalLineTo(228.21f)
                curveTo(235.11f, 352.57f, 239.77f, 359.63f, 237.05f, 365.97f)
                lineTo(225.15f, 393.81f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF00253C)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(385.97f, 361f)
                curveTo(356.5f, 390.45f, 316.53f, 407f, 274.86f, 407f)
                horizontalLineTo(271.79f)
                curveTo(264.89f, 407f, 260.23f, 399.95f, 262.95f, 393.61f)
                lineTo(280.61f, 352.31f)
                lineTo(293.59f, 321.48f)
                curveTo(296.84f, 313.75f, 307.74f, 313.59f, 311.22f, 321.22f)
                lineTo(316.43f, 332.68f)
                curveTo(318.68f, 337.63f, 324.63f, 339.76f, 329.24f, 336.88f)
                curveTo(335.77f, 332.8f, 341.86f, 327.97f, 347.39f, 322.44f)
                curveTo(366.63f, 303.22f, 377.44f, 277.14f, 377.44f, 249.95f)
                curveTo(377.44f, 222.76f, 366.63f, 196.68f, 347.39f, 177.45f)
                curveTo(328.15f, 158.23f, 302.06f, 147.43f, 274.86f, 147.43f)
                lineTo(274.86f, 106.19f)
                lineTo(277.9f, 99.07f)
                curveTo(279.46f, 95.41f, 283.12f, 93.07f, 287.09f, 93.38f)
                curveTo(293.51f, 93.88f, 299.86f, 94.77f, 306.12f, 96.04f)
                curveTo(336.15f, 102.13f, 363.98f, 116.92f, 385.97f, 138.9f)
                curveTo(415.44f, 168.35f, 432f, 208.3f, 432f, 249.95f)
                curveTo(432f, 291.6f, 415.44f, 331.55f, 385.97f, 361f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF00253C)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(274.86f, 106.19f)
                lineTo(274.86f, 147.43f)
                horizontalLineTo(271.79f)
                curveTo(264.89f, 147.43f, 260.23f, 140.38f, 262.95f, 134.03f)
                lineTo(274.86f, 106.19f)
                close()
            }
        }.build()

        return _LogoNoConnect!!
    }

@Suppress("ObjectPropertyName")
private var _LogoNoConnect: ImageVector? = null
