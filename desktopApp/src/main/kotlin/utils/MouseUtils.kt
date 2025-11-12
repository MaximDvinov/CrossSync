package utils

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.MouseInfo
import kotlin.math.max
import kotlin.math.min

fun getMouseAWTPosition(): Pair<Int, Int> {
    val point = MouseInfo.getPointerInfo().location
    return point.x to point.y
}

fun calculateWindowPositionUnderMouse(
    windowStateWidthDp: Dp,
    windowStateHeightDp: Dp,
    density: Density,
    isCenter: Boolean,
): WindowPosition {
    val pointerInfo = MouseInfo.getPointerInfo()
    val mousePoint = pointerInfo.location
    val screenBounds = pointerInfo.device.defaultConfiguration.bounds

    val windowWidthPx = with(density) { windowStateWidthDp.value }.toInt()
    val windowHeightPx = with(density) { windowStateHeightDp.value }.toInt()

    var desiredX = mousePoint.x + if (!isCenter) 30 else -windowWidthPx / 2
    var desiredY = if (!isCenter) mousePoint.y - windowHeightPx / 4 else screenBounds.y + 40

    desiredX =
        max(screenBounds.x, min(desiredX, screenBounds.x + screenBounds.width - windowWidthPx))
    desiredY =
        max(screenBounds.y, min(desiredY, screenBounds.y + screenBounds.height - windowHeightPx))

    return WindowPosition.Absolute(desiredX.dp, desiredY.dp)
}
