package xyz.juncat.media.videolist

import android.util.Log
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.jianjun.base.ext.dp

class VideoListVerticalAdapter :
    RecyclerView.Adapter<VideoListVerticalAdapter.VerticalViewHolder>() {
    private val TAG = "VideoListVerticalAdapte"
    private val dataList = ArrayList<VideoListData>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalViewHolder {
        return VerticalViewHolder(VideoListItem(parent.context).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200f.dp.toInt())
        })
    }

    override fun onBindViewHolder(holder: VerticalViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun update(list: List<VideoListData>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onViewAttachedToWindow(holder: VerticalViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.i(TAG, "onViewAttachedToWindow: ")
    }

    override fun onViewDetachedFromWindow(holder: VerticalViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.i(TAG, "onViewDetachedFromWindow: ")
        val recyclerView = holder.item.videoRecyclerView
        //stop when idle
        recyclerView.children
            .forEach {
                (recyclerView.getChildViewHolder(it) as VideoListHorizontalAdapter.ViewHolder).let {
//                    it.item.stop()
                    VideoPlayerManager.add(it.item)
                }
            }
    }

    class VerticalViewHolder(val item: VideoListItem) : RecyclerView.ViewHolder(item) {

        fun bind(data: VideoListData) {
            if (item.videoRecyclerView.adapter == null) {
                item.videoRecyclerView.adapter = VideoListHorizontalAdapter()
            }
            (item.videoRecyclerView.adapter as? VideoListHorizontalAdapter)?.update(data.list)
            item.title.setText(data.category + " - ${data.list.size}")
        }
    }

}