package com.example.looper_android.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalFrostedSheetOverlay = compositionLocalOf<MutableState<(@Composable () -> Unit)?>?> { null }
val LocalFrostedSheetDragModifier = compositionLocalOf<Modifier> { Modifier }

/**
 * Reusable frosted glass modifier that matches the queue page blurry transparent background.
 */
fun Modifier.frostedGlass(
    hazeState: HazeState?,
    isBlurEnabled: Boolean,
    effectiveDarkTheme: Boolean = true,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    blurRadius: Dp = 20.dp,
    blackAlpha: Float = 0.35f,
    whiteAlpha: Float = 0.0f,
    fallbackColor: Color = Color(0xFF141414)
): Modifier = this
    .clip(shape)
    .then(
        if (isBlurEnabled && hazeState != null) {
            Modifier.hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.Transparent,
                    tints = emptyList(),
                    blurRadius = blurRadius
                )
            )
        } else Modifier
    )
    .background(
        if (isBlurEnabled) Color.Black.copy(alpha = blackAlpha)
        else if (effectiveDarkTheme) Color.Black
        else fallbackColor
    )
    .then(
        if (isBlurEnabled && whiteAlpha > 0f) Modifier.background(Color.White.copy(alpha = whiteAlpha))
        else Modifier
    )
    .border(
        width = borderWidth,
        color = borderColor,
        shape = shape
    )

/**
 * Composable overload of frostedGlass that defaults to LocalHazeState and LocalIsBlurEnabled.
 */
@Composable
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    blurRadius: Dp = 20.dp,
    blackAlpha: Float = 0.35f,
    whiteAlpha: Float = 0.0f,
    effectiveDarkTheme: Boolean = true,
    fallbackColor: Color = Color(0xFF141414)
): Modifier = frostedGlass(
    hazeState = LocalHazeState.current,
    isBlurEnabled = LocalIsBlurEnabled.current,
    effectiveDarkTheme = effectiveDarkTheme,
    shape = shape,
    borderWidth = borderWidth,
    borderColor = borderColor,
    blurRadius = blurRadius,
    blackAlpha = blackAlpha,
    whiteAlpha = whiteAlpha,
    fallbackColor = fallbackColor
)

/**
 * A reusable in-tree frosted glass bottom sheet container.
 * Features:
 * - Real-time frosted glass blur sampling from HazeState
 * - Spring entrance and exit animations
 * - Animated scrim with tap-to-dismiss
 * - 1:1 finger tracking vertical drag-to-dismiss gesture on drag handle and nested scrolling
 * - Back button dismissal support via BackHandler
 * - Automatic navigation bar insets padding
 */
@Composable
fun FrostedBottomSheet(
    visible: Boolean = true,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetHeight: Dp? = null,
    maxHeight: Dp? = null,
    hazeState: HazeState? = LocalHazeState.current,
    isBlurEnabled: Boolean = LocalIsBlurEnabled.current,
    effectiveDarkTheme: Boolean = true,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    blackAlpha: Float = 0.35f,
    showDragHandle: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isDismissing by remember { mutableStateOf(false) }
    val isTargetVisible = visible && !isDismissing

    val transitionState = remember {
        MutableTransitionState(false).apply {
            targetState = isTargetVisible
        }
    }
    LaunchedEffect(isTargetVisible) {
        transitionState.targetState = isTargetVisible
    }

    val transition = rememberTransition(
        transitionState = transitionState,
        label = "FrostedBottomSheetTransition"
    )

    if (!transitionState.currentState && !transitionState.targetState && !visible) {
        return
    }

    BackHandler(enabled = isTargetVisible) {
        if (!isDismissing) {
            isDismissing = true
        }
    }

    val scrimAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(220) else tween(180)
        },
        label = "frosted_scrim_alpha"
    ) { isVis ->
        if (isVis) 0.38f else 0f
    }

    val sheetProgress by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = 520f)
            else spring(dampingRatio = 0.90f, stiffness = 500f)
        },
        label = "frosted_sheet_progress"
    ) { isVis ->
        if (isVis) 1f else 0f
    }

    // When dismissal animation completes, notify caller
    LaunchedEffect(transitionState.isIdle, transitionState.currentState, isDismissing) {
        if (isDismissing && transitionState.isIdle && !transitionState.currentState) {
            onDismiss()
        }
    }

    // Safety timeout in case transition state completes asynchronously
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            delay(350)
            onDismiss()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val dismissThresholdPx = with(density) { 85.dp.toPx() }
    var measuredHeightPx by remember { mutableFloatStateOf(0f) }

    val effectiveHeightPx = when {
        sheetHeight != null -> with(density) { sheetHeight.toPx() }
        measuredHeightPx > 0f -> measuredHeightPx
        else -> screenHeightPx * 0.75f
    }

    fun triggerDismiss() {
        if (!isDismissing) {
            isDismissing = true
        }
    }

    LaunchedEffect(isTargetVisible) {
        if (isTargetVisible) {
            dragOffsetY = 0f
        }
    }

    // 1:1 finger tracking drag gesture
    val headerDraggable = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
        },
        onDragStopped = { velocity ->
            if (dragOffsetY > dismissThresholdPx || velocity > 1000f) {
                triggerDismiss()
            } else {
                coroutineScope.launch {
                    animate(
                        initialValue = dragOffsetY,
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f)
                    ) { value, _ ->
                        dragOffsetY = value
                    }
                }
            }
        }
    )

    // Nested scroll connection for scrollable sheet content
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dragOffsetY > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-dragOffsetY)
                    dragOffsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY > 0f) {
                    if (dragOffsetY > dismissThresholdPx || available.y > 1000f) {
                        triggerDismiss()
                    } else {
                        animate(
                            initialValue = dragOffsetY,
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f)
                        ) { value, _ ->
                            dragOffsetY = value
                        }
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val currentOffsetY = (((1f - sheetProgress) * effectiveHeightPx) + dragOffsetY).coerceAtLeast(0f)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Scrim
        if (scrimAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .then(headerDraggable)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerDismiss()
                    }
            )
        }

        // Sheet Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    when {
                        sheetHeight != null -> Modifier.height(sheetHeight)
                        maxHeight != null -> Modifier.heightIn(max = maxHeight)
                        else -> Modifier.heightIn(max = screenHeight * 0.88f)
                    }
                )
                .offset { IntOffset(0, currentOffsetY.toInt()) }
                .onSizeChanged { size ->
                    if (size.height > 0) {
                        measuredHeightPx = size.height.toFloat()
                    }
                }
                .nestedScroll(nestedScrollConnection)
                .frostedGlass(
                    hazeState = hazeState,
                    isBlurEnabled = isBlurEnabled,
                    effectiveDarkTheme = effectiveDarkTheme,
                    shape = shape,
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    blackAlpha = blackAlpha
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Prevent click-through to underlying scrim or screen content
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Drag handle
                if (dragHandle != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(headerDraggable),
                        contentAlignment = Alignment.Center
                    ) {
                        dragHandle()
                    }
                } else if (showDragHandle) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(headerDraggable)
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                    }
                }

                CompositionLocalProvider(
                    LocalFrostedSheetDragModifier provides headerDraggable
                ) {
                    content()
                }
            }
        }
    }
}
