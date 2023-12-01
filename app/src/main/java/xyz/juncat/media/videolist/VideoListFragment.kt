package xyz.juncat.media.videolist

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoListFragment : Fragment() {

    private val adapter = VideoListVerticalAdapter()
    private val videoRecyclerView by lazy {

        RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VideoListFragment.adapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            recyclerView.post {
                                VideoPlayerManager.clear()
                                recyclerView.children.forEach {
                                    (recyclerView.getChildViewHolder(it) as VideoListVerticalAdapter.VerticalViewHolder)
                                        .item
                                        .videoRecyclerView
                                        .let { childRv ->
                                            childRv
                                                .children
                                                .forEach {
                                                    (childRv.getChildViewHolder(it) as VideoListHorizontalAdapter.ViewHolder)
                                                        .item.restart()
                                                }
                                        }
                                }
                            }

                        }
                        else -> {
                            // the following code will cause ui freeze
//                            recyclerView.children.forEach {
//                                (recyclerView.getChildViewHolder(it) as VideoListVerticalAdapter.VerticalViewHolder)
//                                    .item
//                                    .videoRecyclerView
//                                    .let { childRv ->
//                                        childRv
//                                            .children
//                                            .forEach {
//                                                (childRv.getChildViewHolder(it) as VideoListHorizontalAdapter.ViewHolder)
//                                                    .item.stop()
//                                            }
//                                    }
//                            }
                        }
                    }
                }
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return videoRecyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getParcelable<Uri>(KEY_VIDEO_URI)?.let {
            adapter.update(VideoDataFactory.genVideoListData(it))
        }
    }

    override fun onPause() {
        super.onPause()
        videoRecyclerView.children.forEach {
            (videoRecyclerView.getChildViewHolder(it) as VideoListVerticalAdapter.VerticalViewHolder)
                .item
                .videoRecyclerView
                .let { childRv ->
                    childRv
                        .children
                        .forEach {
                            (childRv.getChildViewHolder(it) as VideoListHorizontalAdapter.ViewHolder)
                                .item.stop()
                        }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        videoRecyclerView.children.forEach {
            (videoRecyclerView.getChildViewHolder(it) as VideoListVerticalAdapter.VerticalViewHolder)
                .item
                .videoRecyclerView
                .let { childRv ->
                    childRv
                        .children
                        .forEach {
                            (childRv.getChildViewHolder(it) as VideoListHorizontalAdapter.ViewHolder)
                                .item.restart()
                        }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoPlayerManager.destroy()
    }

    companion object {
        private const val KEY_VIDEO_URI = "key_video_uri"

        fun newInstance(videoUri: Uri): VideoListFragment {
            return VideoListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_VIDEO_URI, videoUri)
                }
            }
        }
    }
}