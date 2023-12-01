package com.jianjun.base.utils

import android.app.Activity
import android.graphics.Rect

object KeyboardUtils {
    fun Activity.observeKeyboardChange(onChange: (isShowing: Boolean) -> Unit) {
        val rootView = this.window.decorView
        val r = Rect()
        var lastHeight = 0
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            rootView.getWindowVisibleDisplayFrame(r)
            val height = r.height()
            if (lastHeight == 0) {
                lastHeight = height
            } else {
                val diff = lastHeight - height
                if (diff > 200) {
                    onChange(true)
                    lastHeight = height
                } else if (diff < -200) {
                    onChange(false)
                    lastHeight = height
                }
            }
        }
    }
}