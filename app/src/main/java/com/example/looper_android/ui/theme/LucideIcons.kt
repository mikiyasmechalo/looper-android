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
}
