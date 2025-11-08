package utils

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.MouseInfo

fun getMouseAWTPosition(): Pair<Int, Int> {
    val point = MouseInfo.getPointerInfo().location
    return point.x to point.y
}

fun calculateWindowPositionUnderMouse(
    windowStateWidthDp: Dp,
    windowStateHeightDp: Dp,
    density: Density,
    isCenter: Boolean
): WindowPosition {
    val (mx, my) = getMouseAWTPosition()
    val windowWidthPx = with(density) { windowStateWidthDp.toPx() }.toInt()
    val windowHeightPx = with(density) { windowStateHeightDp.toPx() }.toInt()

    val desiredX = mx + if (!isCenter) 30 else - windowWidthPx / 4
    val desiredY =if (!isCenter)  my - windowHeightPx / 8 else 40

    return WindowPosition.Absolute((desiredX).dp, (desiredY).dp)
}