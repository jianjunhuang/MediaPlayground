package com.jianjun.base.view

import android.content.Context
import android.util.AttributeSet
import android.view.FrameMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class CustomViewGroup : ViewGroup {
    protected val layoutQueue = ArrayList<ILayout>()
    protected val measureQueue = ArrayList<IMeasure>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        for (index in measureQueue.indices) {
            measureQueue[index].onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (index in layoutQueue.indices) {
            layoutQueue[index].onLayout(changed, l, t, r, b)
        }
    }


    protected inline fun <T : View> T.added(block: T.() -> Unit): T {
        block()
        addView(this)
        return this
    }

    protected inline fun <T : View> T.added(
        init: T.() -> Unit,
        crossinline onMeasure: T.(widthMeasureSpec: Int, heightMeasureSpec: Int) -> Unit,
        crossinline onLayout: T.(changed: Boolean, l: Int, t: Int, r: Int, b: Int) -> Unit
    ): T {
        create(init, onMeasure, onLayout)
        return this
    }

    protected inline fun <T : View> T.create(
        init: T.() -> Unit,
        crossinline onMeasure: T.(widthMeasureSpec: Int, heightMeasureSpec: Int) -> Unit,
        crossinline onLayout: T.(changed: Boolean, l: Int, t: Int, r: Int, b: Int) -> Unit
    ): T {
        init.invoke(this)
        addView(this)
        measureQueue.add(object : IMeasure {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                onMeasure.invoke(this@create, widthMeasureSpec, heightMeasureSpec)
            }
        })
        layoutQueue.add(object : ILayout {
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                onLayout.invoke(this@create, changed, l, t, r, b)
            }
        })
        return this

    }

    protected interface ILayout {
        fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int)
    }

    protected interface IMeasure {
        fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
    }
}