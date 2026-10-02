package notifications

import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class MacOsPopupPlacementTest {
    @Test
    fun popupStaysOnDisplayAbovePrimary() {
        val anchor = appKitToAwtScreenPoint(Point(700, 1787), primaryScreenHeight = 900)
        val screen = Rectangle(0, -900, 1440, 900)

        assertEquals(Point(700, -887), anchor)
        assertEquals(Point(510, -864), popupPositionBelowAnchor(anchor, screen, 380, 8))
    }

    @Test
    fun popupStaysOnDisplayBelowPrimary() {
        val anchor = appKitToAwtScreenPoint(Point(700, -13), primaryScreenHeight = 900)
        val screen = Rectangle(0, 900, 1440, 900)

        assertEquals(Point(700, 913), anchor)
        assertEquals(Point(510, 936), popupPositionBelowAnchor(anchor, screen, 380, 8))
    }

    @Test
    fun popupIsClampedToDisplayLeftOfPrimary() {
        val anchor = appKitToAwtScreenPoint(Point(-1400, 887), primaryScreenHeight = 900)
        val screen = Rectangle(-1440, 0, 1440, 900)

        assertEquals(Point(-1432, 36), popupPositionBelowAnchor(anchor, screen, 380, 8))
    }

    @Test
    fun popupIsClampedToRightEdgeOfSecondaryDisplay() {
        val anchor = appKitToAwtScreenPoint(Point(2700, 887), primaryScreenHeight = 900)
        val screen = Rectangle(1440, 0, 1280, 900)

        assertEquals(Point(2332, 36), popupPositionBelowAnchor(anchor, screen, 380, 8))
    }

    @Test
    fun missingIconFallsBackInsideResolvedDisplay() {
        assertEquals(
            Point(2332, 36),
            popupPositionBelowAnchor(null, Rectangle(1440, 0, 1280, 900), 380, 8),
        )
    }
}
