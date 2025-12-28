package com.github.konradcz2001.kinootv.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class that overlays a virtual cursor (mouse) on an Activity, controllable via a remote control D-pad.
 * Usage: Override dispatchKeyEvent in the Activity and pass the event to handleKeyEvent.
 * Toggle: Long press the OK (CENTER) button.
 */
class VirtualCursor(private val activity: Activity) {

    private var cursorView: ImageView? = null
    private var isMouseMode = false
    private var cursorX = 0f
    private var cursorY = 0f
    private val speed = 25f // Cursor movement speed

    // Variables to handle long-press on OK button
    private var keyDownTime: Long = 0
    private val LONG_PRESS_TIME = 800L // ms

    init {
        setupCursor()
    }

    private fun setupCursor() {
        val root = activity.window.decorView.rootView as? ViewGroup ?: return

        cursorView = ImageView(activity).apply {
            // Draw a Red Dot using GradientDrawable
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(Color.RED)
            drawable.setStroke(2, Color.WHITE) // Add white border for visibility
            setImageDrawable(drawable)

            // Set fixed dimensions
            val params = FrameLayout.LayoutParams(30, 30)
            layoutParams = params

            visibility = View.GONE

            // Ensure high Z-Index and Elevation to appear above WebView
            elevation = 1000f
            translationZ = 1000f
        }

        root.addView(cursorView)
        Log.d("VirtualCursor", "Cursor added to view: $root")
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        // 1. Detect long press on OK to toggle mouse mode
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (keyDownTime == 0L) keyDownTime = System.currentTimeMillis()
                // If held long enough
                if (System.currentTimeMillis() - keyDownTime > LONG_PRESS_TIME) {
                    toggleMouseMode()
                    keyDownTime = System.currentTimeMillis() + 10000 // Reset to prevent flickering
                    return true
                }
            } else if (action == KeyEvent.ACTION_UP) {
                // If short click and mouse mode active -> Simulate click
                if (isMouseMode && System.currentTimeMillis() - keyDownTime < LONG_PRESS_TIME) {
                    simulateClick()
                    keyDownTime = 0
                    return true
                }
                keyDownTime = 0
            }
        }

        // 2. Handle movement if mouse mode is active
        if (isMouseMode) {
            if (action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> moveCursor(0f, -speed)
                    KeyEvent.KEYCODE_DPAD_DOWN -> moveCursor(0f, speed)
                    KeyEvent.KEYCODE_DPAD_LEFT -> moveCursor(-speed, 0f)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> moveCursor(speed, 0f)
                    else -> return false // Let other keys (e.g., Back) behave normally
                }
                return true // Consume event to prevent default focus jumping
            }
            // Block ACTION_UP for D-pad arrows in mouse mode
            if (action == KeyEvent.ACTION_UP && isNavKey(keyCode)) return true
        }

        return false
    }

    private fun isNavKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }

    private fun toggleMouseMode() {
        isMouseMode = !isMouseMode
        cursorView?.visibility = if (isMouseMode) View.VISIBLE else View.GONE

        if (isMouseMode) {
            // Center the cursor initially
            val displayMetrics = activity.resources.displayMetrics
            cursorX = 150f
            cursorY = displayMetrics.heightPixels / 2f
            updateCursorPosition()
        }
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val displayMetrics = activity.resources.displayMetrics
        cursorX = max(0f, min(displayMetrics.widthPixels.toFloat(), cursorX + dx))
        cursorY = max(0f, min(displayMetrics.heightPixels.toFloat(), cursorY + dy))
        updateCursorPosition()
    }

    private fun updateCursorPosition() {
        cursorView?.x = cursorX
        cursorView?.y = cursorY
    }

    private fun simulateClick() {
        val root = activity.window.decorView.rootView
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 100, MotionEvent.ACTION_UP, cursorX, cursorY, 0)

        root.dispatchTouchEvent(downEvent)
        root.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }
}