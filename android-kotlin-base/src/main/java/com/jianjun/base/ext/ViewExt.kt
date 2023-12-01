package com.jianjun.base.ext

import android.content.res.Resources
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat

// ----------------------------- Measure -------------------------------------------
val Int.measureSize
    get() = View.MeasureSpec.getSize(this)
val Int.measureMode
    get() = View.MeasureSpec.getMode(this)

/**
 * set MeasureSpec.mode to AT_MOST
 */
val Int.toAtMostSpec
    get() = View.MeasureSpec.makeMeasureSpec(this.measureSize, View.MeasureSpec.AT_MOST)

/**
 * set MeasureSpec.mode to EXACTLY
 */
val Int.toExactlySpec
    get() = View.MeasureSpec.makeMeasureSpec(this.measureSize, View.MeasureSpec.EXACTLY)

fun View.measureMatchParent(widthSpec: Int, heightSpec: Int) {
    this.measure(
        widthSpec.measureSize.toExactlySpec,
        heightSpec.measureSize.toExactlySpec
    )
}

fun View.measureWrapContent(widthSpec: Int, heightSpec: Int) {
    this.measure(
        widthSpec.measureSize.toAtMostSpec,
        heightSpec.measureSize.toAtMostSpec
    )
}

fun View.measureExactly(widthSize: Int, heightSize: Int) {
    this.measure(
        widthSize.toExactlySpec,
        heightSize.toExactlySpec
    )
}

fun View.layout(left: Int, top: Int) {
    this.layout(left, top, left + this.measuredWidth, top + this.measuredHeight)
}

fun View.colorInt(colorRes: Int): Int {
    return ResourcesCompat.getColor(resources, colorRes, null)
}

// ---------------------------- Convert ---------------------------------------------
val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this,
        Resources.getSystem().displayMetrics
    )

val Int.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val Float.sp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this,
        Resources.getSystem().displayMetrics
    )

// ---------------------------- Get Size ---------------------------------------------
val Paint.textHeight: Float
    get() {
        val fontMetrics = this.fontMetrics
        return fontMetrics.bottom - fontMetrics.top + fontMetrics.leading
    }

// ----------------------------- Visibility -----------------------------------------------

fun View.visible() {
    if (this.visibility != View.VISIBLE)
        this.visibility = View.VISIBLE
}

fun View.invisible() {
    if (this.visibility != View.INVISIBLE)
        this.visibility = View.INVISIBLE
}

fun View.gone() {
    if (this.visibility != View.GONE)
        this.visibility = View.GONE
}

// ------------------------------------------------------------------

fun View.setMatchParent() {
    setLayoutParamsSize(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
}

fun View.setWrapParent() {
    setLayoutParamsSize(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

fun View.setMatchWidth() {
    setLayoutParamsSize(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

fun View.setLayoutParamsSize(width: Int, height: Int) {
    if (layoutParams != null) {
        layoutParams.width = width
        layoutParams.height = height
    } else {
        layoutParams = ViewGroup.LayoutParams(width, height)
    }
}
