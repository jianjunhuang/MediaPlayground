package xyz.juncat.media.videolist

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

object VideoPlayCounter {
    private const val TAG = "VideoPlayCounter"
    private val counter = AtomicInteger()


    @Synchronized
    fun play() {
        Log.i(TAG, "play: ${counter.incrementAndGet()}")
    }

    @Synchronized
    fun stop() {
        Log.i(TAG, "stop: ${counter.decrementAndGet()}")
    }
}