package com.example.looper_android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LucideIcons {
    private var _githubCustom: ImageVector? = null
    val GithubCustom: ImageVector
        get() {
            if (_githubCustom != null) return _githubCustom!!
            _githubCustom = ImageVector.Builder(
                name = "GithubCustom",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 22f,
                viewportHeight = 22f
            ).apply {
                addPath(
                    fill = SolidColor(Color.White),
                    pathData = androidx.compose.ui.graphics.vector.addPathNodes("M11 0C9.55546 0 8.12506 0.291587 6.79048 0.858113C5.4559 1.42464 4.24327 2.25501 3.22183 3.30181C1.15893 5.41593 0 8.28329 0 11.2731C0 16.2558 3.157 20.4832 7.524 21.9825C8.074 22.0727 8.25 21.7233 8.25 21.4189V19.5137C5.203 20.1901 4.554 18.0031 4.554 18.0031C4.048 16.6955 3.333 16.346 3.333 16.346C2.332 15.6471 3.41 15.6696 3.41 15.6696C4.51 15.7485 5.093 16.8307 5.093 16.8307C6.05 18.5442 7.667 18.037 8.294 17.7664C8.393 17.0336 8.679 16.5376 8.987 16.2558C6.545 15.974 3.982 15.0045 3.982 10.7094C3.982 9.45813 4.4 8.45482 5.115 7.65443C5.005 7.37261 4.62 6.2002 5.225 4.67834C5.225 4.67834 6.149 4.37396 8.25 5.82819C9.119 5.58018 10.065 5.45618 11 5.45618C11.935 5.45618 12.881 5.58018 13.75 5.82819C15.851 4.37396 16.775 4.67834 16.775 4.67834C17.38 6.2002 16.995 7.37261 16.885 7.65443C17.6 8.45482 18.018 9.45813 18.018 10.7094C18.018 15.0158 15.444 15.9627 12.991 16.2445C13.387 16.594 13.75 17.2817 13.75 18.3301V21.4189C13.75 21.7233 13.926 22.084 14.487 21.9825C18.854 20.4719 22 16.2558 22 11.2731C22 9.79269 21.7155 8.32678 21.1627 6.95907C20.6099 5.59135 19.7996 4.34862 18.7782 3.30181C17.7567 2.25501 16.5441 1.42464 15.2095 0.858113C13.8749 0.291587 12.4445 0 11 0Z")
                )
            }.build()
            return _githubCustom!!
        }

    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9f)
                lineTo(12f, 2f)
                lineTo(21f, 9f)
                verticalLineTo(20f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                horizontalLineTo(5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 22f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                verticalLineTo(22f)
            }
        }.build()

    val Search: ImageVector
        get() = ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 3f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 8f, dy1 = 8f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -8f, dy1 = 8f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -8f, dy1 = -8f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 8f, dy1 = -8f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = -3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = -3f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.4f, 15f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.33f, dy1 = 1.82f)
                lineToRelative(0.06f, 0.06f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = 2.83f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.83f, dy1 = 0f)
                lineToRelative(-0.06f, -0.06f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.82f, dy1 = -0.33f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1f, dy1 = 1.51f)
                verticalLineTo(21f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                verticalLineToRelative(-0.09f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1f, dy1 = -1.51f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.82f, dy1 = 0.33f)
                lineToRelative(-0.06f, 0.06f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.83f, dy1 = 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = -2.83f)
                lineToRelative(0.06f, -0.06f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.33f, dy1 = -1.82f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.51f, dy1 = -1f)
                horizontalLineTo(3f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                horizontalLineToRelative(0.09f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.51f, dy1 = -1f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.33f, dy1 = -1.82f)
                lineToRelative(-0.06f, -0.06f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = -2.83f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.83f, dy1 = 0f)
                lineToRelative(0.06f, 0.06f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.82f, dy1 = 0.33f)
                horizontalLineTo(9f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1f, dy1 = -1.51f)
                verticalLineTo(3f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                verticalLineToRelative(0.09f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1f, dy1 = 1.51f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.82f, dy1 = -0.33f)
                lineToRelative(0.06f, -0.06f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.83f, dy1 = 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = 2.83f)
                lineToRelative(-0.06f, 0.06f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.33f, dy1 = 1.82f)
                verticalLineTo(9f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.51f, dy1 = 1f)
                horizontalLineTo(21f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                horizontalLineToRelative(-0.09f)
                arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.51f, dy1 = 1f)
                close()
            }
        }.build()

    val Library: ImageVector
        get() = ImageVector.Builder(
            name = "Library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 6f)
                lineToRelative(4f, 14f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6f)
                verticalLineToRelative(14f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 8f)
                verticalLineToRelative(12f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                verticalLineToRelative(16f)
            }
        }.build()

    val Music: ImageVector
        get() = ImageVector.Builder(
            name = "Music",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                verticalLineTo(5f)
                lineToRelative(12f, -2f)
                verticalLineToRelative(13f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 15f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = -3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = -3f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 13f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3f, dy1 = -3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = -3f)
            }
        }.build()

    val Shuffle: ImageVector
        get() = ImageVector.Builder(
            name = "Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 3f)
                lineToRelative(5f, 0f)
                lineToRelative(0f, 5f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                lineTo(21f, 3f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 16f)
                lineToRelative(0f, 5f)
                lineToRelative(-5f, 0f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 15f)
                lineTo(21f, 21f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(9f, 9f)
            }
        }.build()

    val MoreVertical: ImageVector
        get() = ImageVector.Builder(
            name = "MoreVertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = -1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = -1f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 4f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = -1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = -1f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = 1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1f, dy1 = -1f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1f, dy1 = -1f)
            }
        }.build()

    val PlayCircle: ImageVector
        get() = ImageVector.Builder(
            name = "PlayCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -20f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 20f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 8f)
                lineToRelative(6f, 4f)
                lineToRelative(-6f, 4f)
                close()
            }
        }.build()

    val ListPlus: ImageVector
        get() = ImageVector.Builder(
            name = "ListPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 12f)
                horizontalLineTo(3f)
                moveToRelative(18f, 0f)
                horizontalLineToRelative(-3f)
                moveToRelative(0f, -3f)
                verticalLineToRelative(6f)
                moveToRelative(-7f, -12f)
                horizontalLineTo(3f)
                moveToRelative(13f, 12f)
                horizontalLineTo(3f)
            }
        }.build()

    val ListMusic: ImageVector
        get() = ImageVector.Builder(
            name = "ListMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 15f)
                verticalLineTo(6f)
                moveToRelative(-9f, 6f)
                horizontalLineTo(3f)
                moveToRelative(9f, -6f)
                horizontalLineTo(3f)
                moveToRelative(9f, 12f)
                horizontalLineTo(3f)
                moveToRelative(15f, -3f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -4f, dy1 = 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 4f, dy1 = 0f)
                close()
            }
        }.build()

    val Timer: ImageVector
        get() = ImageVector.Builder(
            name = "Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 2f)
                horizontalLineToRelative(4f)
                moveToRelative(-2f, 12f)
                lineToRelative(3f, -3f)
                moveToRelative(6f, 3f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -18f, dy1 = 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 18f, dy1 = 0f)
                close()
            }
        }.build()

    val Trash2: ImageVector
        get() = ImageVector.Builder(
            name = "Trash2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6f)
                horizontalLineToRelative(18f)
                moveToRelative(-2f, 0f)
                verticalLineToRelative(14f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                horizontalLineTo(7f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                verticalLineTo(6f)
                moveToRelative(3f, 0f)
                verticalLineTo(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                horizontalLineToRelative(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                verticalLineToRelative(2f)
                moveToRelative(-6f, 5f)
                verticalLineToRelative(6f)
                moveToRelative(4f, -6f)
                verticalLineToRelative(6f)
            }
        }.build()

    val Edit3: ImageVector
        get() = ImageVector.Builder(
            name = "Edit3",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20f)
                horizontalLineToRelative(9f)
                moveToRelative(-4.5f, -15.5f)
                arcToRelative(2.121f, 2.121f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 3f)
                lineTo(7f, 18f)
                lineToRelative(-4f, 1f)
                lineToRelative(1f, -4f)
                lineTo(16.5f, 4.5f)
                close()
            }
        }.build()

    val Sliders: ImageVector
        get() = ImageVector.Builder(
            name = "Sliders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 21f)
                verticalLineToRelative(-7f)
                moveToRelative(0f, -4f)
                verticalLineTo(3f)
                moveToRelative(8f, 18f)
                verticalLineToRelative(-9f)
                moveToRelative(0f, -4f)
                verticalLineTo(3f)
                moveToRelative(8f, 18f)
                verticalLineToRelative(-5f)
                moveToRelative(0f, -4f)
                verticalLineTo(3f)
                moveTo(1f, 14f)
                horizontalLineToRelative(6f)
                moveToRelative(2f, -4f)
                horizontalLineToRelative(6f)
                moveToRelative(2f, 8f)
                horizontalLineToRelative(6f)
            }
        }.build()

    val Info: ImageVector
        get() = ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -20f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 20f)
                close()
                moveToRelative(0f, -14f)
                horizontalLineToRelative(0.01f)
                moveToRelative(-0.01f, 4f)
                verticalLineToRelative(4f)
            }
        }.build()

    val Activity: ImageVector
        get() = ImageVector.Builder(
            name = "Activity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 12f)
                horizontalLineToRelative(-4f)
                lineToRelative(-3f, 9f)
                lineToRelative(-6f, -18f)
                lineToRelative(-3f, 9f)
                horizontalLineTo(2f)
            }
        }.build()

    val Share2: ImageVector
        get() = ImageVector.Builder(
            name = "Share2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -6f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 6f)
                close()
                moveToRelative(-12f, 7f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -6f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 6f)
                close()
                moveToRelative(12f, 7f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -6f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 6f)
                close()
                moveToRelative(-9.41f, -4.59f)
                lineToRelative(6.83f, 3.98f)
                moveToRelative(0f, -10.78f)
                lineToRelative(-6.83f, 3.98f)
            }
        }.build()

    val Heart: ImageVector
        get() = ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 14f)
                curveTo(20.49f, 12.54f, 22f, 10.79f, 22f, 8.5f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -9.5f, dy1 = -3.88f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -9.5f, dy1 = 3.88f)
                curveTo(3f, 10.79f, 4.51f, 12.54f, 6f, 14f)
                lineToRelative(6f, 6f)
                close()
            }
        }.build()

    val Moon: ImageVector
        get() = ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12.79f)
                curveTo(20.13f, 13.56f, 19.02f, 14f, 17.82f, 14f)
                curveTo(13.95f, 14f, 10.82f, 10.87f, 10.82f, 7f)
                curveTo(10.82f, 5.8f, 11.26f, 4.69f, 12.03f, 3.82f)
                curveTo(7.48f, 4.38f, 4f, 8.27f, 4f, 13f)
                curveTo(4f, 18.52f, 8.48f, 23f, 14f, 23f)
                curveTo(18.73f, 23f, 22.62f, 19.52f, 23.18f, 14.97f)
                close()
            }
        }.build()

    val Eye: ImageVector
        get() = ImageVector.Builder(
            name = "Eye",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 12f)
                curveTo(3.5f, 7f, 7.5f, 4f, 12f, 4f)
                curveTo(16.5f, 4f, 20.5f, 7f, 23f, 12f)
                curveTo(20.5f, 17f, 16.5f, 20f, 12f, 20f)
                curveTo(7.5f, 20f, 3.5f, 17f, 1f, 12f)
                close()
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
            }
        }.build()

    val Disc: ImageVector
        get() = ImageVector.Builder(
            name = "Disc",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
            }
        }.build()

    val Layers: ImageVector
        get() = ImageVector.Builder(
            name = "Layers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(2f, 7f)
                lineTo(12f, 12f)
                lineTo(22f, 7f)
                close()
                moveTo(2f, 12f)
                lineTo(12f, 17f)
                lineTo(22f, 12f)
                moveTo(2f, 17f)
                lineTo(12f, 22f)
                lineTo(22f, 17f)
            }
        }.build()

    val Clock: ImageVector
        get() = ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(12f, 6f)
                verticalLineTo(12f)
                lineTo(16f, 14f)
            }
        }.build()

    val Power: ImageVector
        get() = ImageVector.Builder(
            name = "Power",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.36f, 6.64f)
                curveTo(20.61f, 8.89f, 21.05f, 12.35f, 19.46f, 15.07f)
                curveTo(17.87f, 17.79f, 14.6f, 19.16f, 11.64f, 18.36f)
                curveTo(8.68f, 17.56f, 6.64f, 14.77f, 6.64f, 11.64f)
                moveTo(12f, 2f)
                verticalLineTo(12f)
            }
        }.build()

    val Droplet: ImageVector
        get() = ImageVector.Builder(
            name = "Droplet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(15.87f, 22f, 19f, 18.87f, 19f, 15f)
                curveTo(19f, 13f, 18f, 11.1f, 16f, 9.5f)
                lineTo(12f, 2f)
                lineTo(8f, 9.5f)
                curveTo(6f, 11.1f, 5f, 13f, 5f, 15f)
                curveTo(5f, 18.87f, 8.13f, 22f, 12f, 22f)
                close()
            }
        }.build()

    val AlertCircle: ImageVector
        get() = ImageVector.Builder(
            name = "AlertCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(12f)
                moveTo(12f, 16f)
                horizontalLineTo(12.01f)
            }
        }.build()

    val PauseCircle: ImageVector
        get() = ImageVector.Builder(
            name = "PauseCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(10f, 15f)
                verticalLineTo(9f)
                moveTo(14f, 15f)
                verticalLineTo(9f)
            }
        }.build()

    val StopCircle: ImageVector
        get() = ImageVector.Builder(
            name = "StopCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(9f, 9f)
                horizontalLineTo(15f)
                verticalLineTo(15f)
                horizontalLineTo(9f)
                close()
            }
        }.build()

    val UserCheck: ImageVector
        get() = ImageVector.Builder(
            name = "UserCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 16.79f, 14.21f, 15f, 12f, 15f)
                horizontalLineTo(5f)
                curveTo(2.79f, 15f, 1f, 16.79f, 1f, 19f)
                verticalLineTo(21f)
                moveTo(8.5f, 11f)
                curveTo(10.71f, 11f, 12.5f, 9.21f, 12.5f, 7f)
                curveTo(12.5f, 4.79f, 10.71f, 3f, 8.5f, 3f)
                curveTo(6.29f, 3f, 4.5f, 4.79f, 4.5f, 7f)
                curveTo(4.5f, 9.21f, 6.29f, 11f, 8.5f, 11f)
                close()
                moveTo(17f, 11f)
                lineTo(19f, 13f)
                lineTo(23f, 9f)
            }
        }.build()

    val DownloadCloud: ImageVector
        get() = ImageVector.Builder(
            name = "DownloadCloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 17f)
                lineTo(12f, 21f)
                lineTo(16f, 17f)
                moveTo(12f, 12f)
                verticalLineTo(21f)
                moveTo(20.88f, 18.09f)
                curveTo(21.57f, 16.71f, 21.72f, 15.12f, 21.3f, 13.62f)
                curveTo(20.88f, 12.12f, 19.92f, 10.82f, 18.59f, 9.98f)
                horizontalLineTo(16.74f)
                curveTo(16.03f, 7.37f, 14.34f, 5.17f, 11.96f, 3.82f)
                curveTo(9.58f, 2.47f, 6.74f, 2.08f, 4.09f, 2.74f)
            }
        }.build()
}
