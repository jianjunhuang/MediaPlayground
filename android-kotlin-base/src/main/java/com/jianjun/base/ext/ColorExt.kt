package com.jianjun.base.ext

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

val Int.isDark
    get() = ColorUtils.calculateLuminance(this) > 0.5

/**
 * return color string, format is '#xxxxxx'
 */
val Int.toHexColor
    get() = String.format("#%06X", 0xFFFFFF and this)

val Int.toHexColorAlpha
    get() = String.format("#%08X", 0xFFFFFFFF and this.toLong())

/**
 * change the alpha with color
 * @param alpha -> [0f, 1f]
 */
inline fun Int.colorWithAlpha(alpha: Float): Int {
    if (alpha !in 0f..1f) throw IllegalArgumentException("alpha must in [0f, 1f]")
    return (this and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
}
