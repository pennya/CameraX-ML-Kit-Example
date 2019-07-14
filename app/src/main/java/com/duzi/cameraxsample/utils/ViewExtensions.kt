package com.duzi.cameraxsample.utils

import android.view.View
import android.widget.ImageButton

const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()  // 사용자가 클릭한 것과 같은 효과
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}