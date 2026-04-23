import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.cross.sync.theme.icons.AppIcons

val AppIcons.Settings: ImageVector
    get() {
        if (_SettingIconlyPro != null) {
            return _SettingIconlyPro!!
        }
        _SettingIconlyPro = ImageVector.Builder(
            name = "SettingIconlyPro",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF02609B)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(8.75f, 12f)
                curveTo(8.75f, 13.792f, 10.208f, 15.25f, 12f, 15.25f)
                curveTo(13.792f, 15.25f, 15.25f, 13.792f, 15.25f, 12f)
                curveTo(15.25f, 10.208f, 13.792f, 8.75f, 12f, 8.75f)
                curveTo(10.208f, 8.75f, 8.75f, 10.208f, 8.75f, 12f)
                close()
                moveTo(10.25f, 12f)
                curveTo(10.25f, 11.035f, 11.035f, 10.25f, 12f, 10.25f)
                curveTo(12.965f, 10.25f, 13.75f, 11.035f, 13.75f, 12f)
                curveTo(13.75f, 12.965f, 12.965f, 13.75f, 12f, 13.75f)
                curveTo(11.035f, 13.75f, 10.25f, 12.965f, 10.25f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF02609B)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(8.775f, 19.008f)
                curveTo(8.775f, 20.796f, 10.222f, 22.25f, 12f, 22.25f)
                curveTo(13.778f, 22.25f, 15.225f, 20.796f, 15.225f, 19.008f)
                curveTo(15.225f, 18.585f, 15.505f, 18.378f, 15.626f, 18.308f)
                curveTo(15.745f, 18.241f, 16.056f, 18.106f, 16.412f, 18.311f)
                curveTo(17.157f, 18.745f, 18.025f, 18.859f, 18.857f, 18.637f)
                curveTo(19.691f, 18.413f, 20.388f, 17.875f, 20.819f, 17.123f)
                curveTo(21.706f, 15.577f, 21.176f, 13.592f, 19.639f, 12.698f)
                curveTo(19.28f, 12.488f, 19.241f, 12.14f, 19.241f, 12f)
                curveTo(19.241f, 11.86f, 19.28f, 11.512f, 19.64f, 11.303f)
                curveTo(21.176f, 10.41f, 21.705f, 8.425f, 20.819f, 6.877f)
                curveTo(20.387f, 6.124f, 19.69f, 5.587f, 18.856f, 5.363f)
                curveTo(18.024f, 5.143f, 17.157f, 5.258f, 16.413f, 5.69f)
                curveTo(16.057f, 5.895f, 15.743f, 5.759f, 15.626f, 5.691f)
                curveTo(15.505f, 5.622f, 15.225f, 5.415f, 15.225f, 4.992f)
                curveTo(15.225f, 3.204f, 13.778f, 1.75f, 12f, 1.75f)
                curveTo(10.222f, 1.75f, 8.775f, 3.204f, 8.775f, 4.992f)
                curveTo(8.775f, 5.415f, 8.495f, 5.622f, 8.374f, 5.691f)
                curveTo(8.256f, 5.762f, 7.943f, 5.896f, 7.589f, 5.69f)
                curveTo(6.844f, 5.258f, 5.977f, 5.143f, 5.144f, 5.363f)
                curveTo(4.31f, 5.587f, 3.613f, 6.124f, 3.181f, 6.877f)
                curveTo(2.295f, 8.425f, 2.824f, 10.41f, 4.36f, 11.303f)
                curveTo(4.72f, 11.512f, 4.758f, 11.86f, 4.758f, 12f)
                curveTo(4.758f, 12.139f, 4.72f, 12.488f, 4.36f, 12.697f)
                curveTo(2.824f, 13.592f, 2.295f, 15.577f, 3.181f, 17.123f)
                curveTo(3.612f, 17.875f, 4.309f, 18.412f, 5.143f, 18.637f)
                curveTo(5.974f, 18.859f, 6.843f, 18.744f, 7.59f, 18.31f)
                curveTo(7.943f, 18.105f, 8.256f, 18.241f, 8.374f, 18.308f)
                curveTo(8.494f, 18.378f, 8.775f, 18.585f, 8.775f, 19.008f)
                close()
                moveTo(9.121f, 17.009f)
                curveTo(8.765f, 16.804f, 8.374f, 16.701f, 7.983f, 16.701f)
                curveTo(7.588f, 16.701f, 7.194f, 16.805f, 6.835f, 17.014f)
                curveTo(6.437f, 17.244f, 5.974f, 17.306f, 5.532f, 17.187f)
                curveTo(5.087f, 17.068f, 4.713f, 16.78f, 4.483f, 16.377f)
                curveTo(4.004f, 15.544f, 4.288f, 14.474f, 5.115f, 13.994f)
                curveTo(5.831f, 13.577f, 6.258f, 12.831f, 6.258f, 12f)
                curveTo(6.258f, 11.169f, 5.831f, 10.423f, 5.114f, 10.006f)
                curveTo(4.289f, 9.525f, 4.005f, 8.457f, 4.482f, 7.623f)
                curveTo(4.713f, 7.22f, 5.087f, 6.931f, 5.532f, 6.812f)
                curveTo(5.976f, 6.696f, 6.438f, 6.757f, 6.836f, 6.987f)
                curveTo(7.551f, 7.403f, 8.406f, 7.404f, 9.122f, 6.992f)
                curveTo(9.844f, 6.576f, 10.275f, 5.829f, 10.275f, 4.992f)
                curveTo(10.275f, 4.031f, 11.049f, 3.25f, 12f, 3.25f)
                curveTo(12.951f, 3.25f, 13.725f, 4.031f, 13.725f, 4.992f)
                curveTo(13.725f, 5.828f, 14.155f, 6.575f, 14.877f, 6.991f)
                curveTo(15.594f, 7.404f, 16.449f, 7.401f, 17.166f, 6.987f)
                curveTo(17.563f, 6.757f, 18.024f, 6.694f, 18.468f, 6.812f)
                curveTo(18.914f, 6.931f, 19.287f, 7.22f, 19.518f, 7.623f)
                curveTo(19.995f, 8.457f, 19.712f, 9.526f, 18.885f, 10.006f)
                curveTo(18.169f, 10.423f, 17.742f, 11.169f, 17.742f, 12f)
                curveTo(17.742f, 12.831f, 18.169f, 13.577f, 18.885f, 13.994f)
                curveTo(19.712f, 14.474f, 19.996f, 15.544f, 19.518f, 16.377f)
                curveTo(19.287f, 16.78f, 18.914f, 17.068f, 18.468f, 17.187f)
                curveTo(18.025f, 17.306f, 17.563f, 17.244f, 17.166f, 17.014f)
                curveTo(16.451f, 16.598f, 15.595f, 16.597f, 14.878f, 17.009f)
                curveTo(14.156f, 17.424f, 13.725f, 18.172f, 13.725f, 19.008f)
                curveTo(13.725f, 19.969f, 12.951f, 20.75f, 12f, 20.75f)
                curveTo(11.049f, 20.75f, 10.275f, 19.969f, 10.275f, 19.008f)
                curveTo(10.275f, 18.171f, 9.844f, 17.424f, 9.121f, 17.009f)
                close()
            }
        }.build()

        return _SettingIconlyPro!!
    }

@Suppress("ObjectPropertyName")
private var _SettingIconlyPro: ImageVector? = null
