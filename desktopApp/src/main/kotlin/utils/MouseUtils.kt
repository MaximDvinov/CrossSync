package utils

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.MouseInfo

fun getMouseAWTPosition(): Pair<Int, Int> {
    val point = MouseInfo.getPointerInfo().location
    return point.x to point.y
}

fun calculateWindowPositionUnderMouse(
    windowStateWidthDp: androidx.compose.ui.unit.Dp,
    windowStateHeightDp: androidx.compose.ui.unit.Dp,
    density: androidx.compose.ui.unit.Density
): WindowPosition {
    val (mx, my) = getMouseAWTPosition()
    val windowWidthPx = with(density) { windowStateWidthDp.toPx() }.toInt()
    val windowHeightPx = with(density) { windowStateHeightDp.toPx() }.toInt()

    var desiredX = mx - windowWidthPx / 4 // центрируем по X
    var desiredY = my - windowHeightPx / 4

    return WindowPosition.Absolute((desiredX).dp, (desiredY).dp)
}