package com.example.looper_android.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val LikedIcon: ImageVector
        get() {
            if (_likedIcon != null) {
                return _likedIcon!!
            }
            _likedIcon = ImageVector.Builder(
                name = "LikedIcon",
                defaultWidth = 16.dp,
                defaultHeight = 15.dp,
                viewportWidth = 16f,
                viewportHeight = 15f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12.4499f, 15f)
                    curveTo(12.3202f, 14.9997f, 12.1925f, 14.9684f, 12.0778f, 14.9088f)
                    lineTo(8f, 12.8013f)
                    lineTo(3.92215f, 14.9088f)
                    curveTo(3.79012f, 14.9768f, 3.64138f, 15.0072f, 3.49272f, 14.9965f)
                    curveTo(3.34405f, 14.9859f, 3.20137f, 14.9346f, 3.08076f, 14.8485f)
                    curveTo(2.96014f, 14.7624f, 2.8664f, 14.6449f, 2.81009f, 14.5093f)
                    curveTo(2.75379f, 14.3736f, 2.73716f, 14.2252f, 2.76208f, 14.0807f)
                    lineTo(3.54053f, 9.61726f)
                    lineTo(0.241133f, 6.45603f)
                    curveTo(0.134398f, 6.3536f, 0.0589249f, 6.22387f, 0.0232352f, 6.08151f)
                    curveTo(-0.0124545f, 5.93914f, -0.00693905f, 5.78981f, 0.0391587f, 5.65036f)
                    curveTo(0.0852565f, 5.51091f, 0.170101f, 5.38691f, 0.28411f, 5.29236f)
                    curveTo(0.39812f, 5.1978f, 0.536755f, 5.13646f, 0.684359f, 5.11526f)
                    lineTo(5.24383f, 4.46414f)
                    lineTo(7.28316f, 0.403292f)
                    curveTo(7.35662f, 0.280493f, 7.46144f, 0.178684f, 7.58725f, 0.107947f)
                    curveTo(7.71305f, 0.0372105f, 7.85547f, 0f, 8.0004f, 0f)
                    curveTo(8.14533f, 0f, 8.28775f, 0.0372105f, 8.41356f, 0.107947f)
                    curveTo(8.53936f, 0.178684f, 8.64418f, 0.280493f, 8.71764f, 0.403292f)
                    lineTo(10.7562f, 4.46414f)
                    lineTo(15.3156f, 5.11526f)
                    curveTo(15.4632f, 5.13646f, 15.6019f, 5.1978f, 15.7159f, 5.29236f)
                    curveTo(15.8299f, 5.38691f, 15.9147f, 5.51091f, 15.9608f, 5.65036f)
                    curveTo(16.0069f, 5.78981f, 16.0125f, 5.93914f, 15.9768f, 6.08151f)
                    curveTo(15.9411f, 6.22387f, 15.8656f, 6.3536f, 15.7589f, 6.45603f)
                    lineTo(12.4595f, 9.61726f)
                    lineTo(13.2387f, 14.0807f)
                    curveTo(13.2584f, 14.1936f, 13.2528f, 14.3092f, 13.2223f, 14.4197f)
                    curveTo(13.1917f, 14.5302f, 13.137f, 14.6328f, 13.062f, 14.7203f)
                    curveTo(12.9869f, 14.8079f, 12.8934f, 14.8783f, 12.7878f, 14.9266f)
                    curveTo(12.6822f, 14.9749f, 12.5663f, 14.9999f, 12.4499f, 15f)
                    close()
                }
            }.build()
            return _likedIcon!!
        }

    private var _likedIcon: ImageVector? = null
}
