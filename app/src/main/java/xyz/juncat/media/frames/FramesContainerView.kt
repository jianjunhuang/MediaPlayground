package xyz.juncat.media.frames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FramesContainerView : View {

    private val frameList = ArrayList<Bitmap>()
    private val frameRectF = RectF()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        measureFrames()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frameRectF.offsetTo(0f, 0f)
        frameList.forEach {
            canvas?.drawBitmap(it, null, frameRectF, null)
            frameRectF.offset(frameRectF.width(), 0f)
        }
    }

    fun setFrames(frames: List<Bitmap>) {
        frameList.clear()
        frameList.addAll(frames)
        measureFrames()
        postInvalidate()
    }

    private fun measureFrames() {
        val dstWidth = width.toFloat() / frameList.size
        frameRectF.set(0f, 0f, dstWidth, height.toFloat())
    }
}