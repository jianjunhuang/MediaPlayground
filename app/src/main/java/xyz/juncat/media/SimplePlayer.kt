package xyz.juncat.media

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.*

class SimplePlayer private constructor(
    private val context: Context,
    private val lifecycle: Lifecycle?,
    private val uri: Uri?,
    private val textureView: TextureView
) : LifecycleObserver {

    private var player: SimpleExoPlayer? = null

    private var callback: Callback? = null

    private val listener = object : Player.Listener {
//        override fun onPlayerError(error: ExoPlaybackException) {
//            super.onPlayerError(error)
//            error.printStackTrace()
//        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            callback?.onPlayingCallback(isPlaying)
        }
    }

    init {
        lifecycle?.addObserver(this)
        initPlayer()
    }

    private fun initPlayer() {
        stop()
        uri?.let {
            player = SimpleExoPlayer.Builder(context)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                .build()
            val mediaItem = MediaItem.fromUri(it)
            player?.setMediaItem(mediaItem)
            player?.setVideoTextureView(textureView)
            player?.repeatMode = Player.REPEAT_MODE_ALL
            player?.addListener(listener)
            player?.playWhenReady = false
            player?.prepare()
        }
    }

    fun play() {
        initPlayer()
        player?.playWhenReady = true
        player?.play()
    }

    fun resume() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun stop() {
        player?.stop()
        player?.release()
        player?.setVideoTextureView(null)
        player?.removeListener(listener)
        player = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        stop()
    }

    class Builder(private val context: Context) {

        private var lifecycle: Lifecycle? = null
        private var uri: Uri? = null
        private var view: TextureView? = null
        private var callback: Callback? = null

        fun registerLifecycle(lifecycle: Lifecycle): Builder {
            this.lifecycle = lifecycle
            return this
        }

        fun setVideoUri(uri: Uri?): Builder {
            this.uri = uri
            return this
        }

        fun setVideoView(view: TextureView): Builder {
            this.view = view
            return this
        }

        fun setCallback(callback: Callback): Builder {
            this.callback = callback
            return this
        }

        fun build(): SimplePlayer {
            return SimplePlayer(context, lifecycle, uri, view ?: TextureView(context)).apply {
                callback?.let {
                    setCallback(it)
                }
            }
        }
    }

    interface Callback {
        fun onPlayingCallback(isPlaying: Boolean)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }
}