package com.example.looper_android.ui.utils

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

object HapticHelper {
    // Configurable haptics flag (can be updated from user settings)
    var isHapticsEnabled: Boolean = false

    fun performLightImpact(view: View) {
        if (!isHapticsEnabled) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performMediumImpact(view: View) {
        if (!isHapticsEnabled) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performSelectionClick(view: View) {
        if (!isHapticsEnabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberHapticFeedbackHelper(): HapticFeedbackController {
    val view = LocalView.current
    return HapticFeedbackController(view)
}

class HapticFeedbackController(private val view: View) {
    fun lightImpact() {
        HapticHelper.performLightImpact(view)
    }

    fun mediumImpact() {
        HapticHelper.performMediumImpact(view)
    }

    fun selectionClick() {
        HapticHelper.performSelectionClick(view)
    }
}
