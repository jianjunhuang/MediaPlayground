package xyz.juncat.media.videolist

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jianjun.base.ext.*
import com.jianjun.base.view.CustomViewGroup

class VideoListItem : CustomViewGroup {

    val title = TextView(context).added {
        gravity = Gravity.CENTER
    }

    val videoRecyclerView = RecyclerView(context).added {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
            initialPrefetchItemCount = 2
        }
        setHasFixedSize(true)
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.left = 16f.dp.toInt()
            }
        })
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        recyclerView.post {
                            VideoPlayerManager.clear()
                            recyclerView.children
                                .forEach {
                                    (recyclerView.getChildViewHolder(it)
                                            as? VideoListHorizontalAdapter.ViewHolder)?.let { holder ->
                                        holder.item.restart()
                                    }
                                }
                        }
                    }
                    else -> {
                        // the following code will cause ui freeze
//                        recyclerView.children
//                            .forEach {
//                                (recyclerView.getChildViewHolder(it)
//                                        as? VideoListHorizontalAdapter.ViewHolder)?.let { holder ->
//                                    holder.item.stop()
//                                }
//                            }
                    }
                }
            }
        })
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        title.measure(measuredWidth.toExactlySpec, measuredHeight.toAtMostSpec)
        videoRecyclerView.measureExactly(
            measuredWidth,
            measuredHeight - title.measuredHeight
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        title.layout(0, 0, title.measuredWidth, title.measuredHeight)
        videoRecyclerView.layout(
            0, title.bottom, width, height
        )
    }
}