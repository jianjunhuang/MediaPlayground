package xyz.juncat.media.videolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jianjun.base.ext.dp
import com.jianjun.base.ext.invisible

class VideoListHorizontalAdapter : RecyclerView.Adapter<VideoListHorizontalAdapter.ViewHolder>() {

    private val dataList = ArrayList<VideoData>()
    private val TAG = "VideoListHorizontal"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val dstHeight = 180f.dp
        val dstWidth = dstHeight * when (viewType) {
            1 -> {
                4f / 3f
            }
            2 -> {
                16f / 9f
            }
            else -> {
                1f
            }
        }
        return ViewHolder(VideoItemView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(dstWidth.toInt(), dstHeight.toInt())
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataList[position].aspectRatio) {
            1f -> {
                0
            }
            4f / 3f -> {
                1
            }
            16f / 9f -> {
                2
            }
            else -> {
                0
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun update(list: List<VideoData>) {
        //todo diff
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.i(TAG, "onViewAttachedToWindow: ")
        val recyclerView = holder.itemView.parent
        val parentRecyclerView = recyclerView.parent.parent as RecyclerView
        if (recyclerView is RecyclerView
            && (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE
                    || parentRecyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE)
        ) {
            //for resume video
            holder.item.setVideoUri(dataList[holder.adapterPosition].videoUri)
            holder.item.textureView.invisible()
        } else {
            holder.item.play(dataList[holder.adapterPosition].videoUri)
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.i(TAG, "onViewDetachedFromWindow: ")
//        holder.item.stop()
        VideoPlayerManager.add(holder.item)
    }

    class ViewHolder(val item: VideoItemView) : RecyclerView.ViewHolder(item) {

        fun bind(data: VideoData) {
            Glide.with(item.cover)
                .load(data.videoUri)
                .centerCrop()
                .placeholder(ColorDrawable(Color.LTGRAY))
                .into(item.cover)
            //todo play video
        }
    }
}


