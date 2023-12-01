package xyz.juncat.media.videolist

import java.lang.ref.WeakReference
import java.util.*

/**
 * delay player.release() to prevent ui freezing
 */
object VideoPlayerManager {

    private val pendingReleaseQueue = LinkedList<WeakReference<VideoItemView>>()
    private const val TAG = "VideoPlayerManager"
    fun add(player: VideoItemView) {
        pendingReleaseQueue.add(WeakReference(player))
    }

    fun clear() {
        while (pendingReleaseQueue.isNotEmpty()) {
            pendingReleaseQueue.pollFirst()?.get()?.stop()
        }
    }

    fun destroy() {
        pendingReleaseQueue.clear()
    }

    const val PLAYER_EXO = 0
    const val PLAYER_IJK = 1

    /**
     * rebuild VideoListFragment to apply changed
     */
    var selectedPlayer: Int = PLAYER_EXO
}