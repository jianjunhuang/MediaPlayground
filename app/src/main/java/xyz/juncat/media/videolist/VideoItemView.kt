package xyz.juncat.media.videolist

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import com.jianjun.base.ext.invisible
import com.jianjun.base.ext.visible
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import xyz.juncat.media.SimplePlayer
import xyz.juncat.media.player.SimpleExoMediaPlayer

class VideoItemView : ViewGroup, SimplePlayer.Callback {

    val cover = ImageView(context).apply {
        this@VideoItemView.addView(this)
    }
    var player: IMediaPlayer? = null
    private var playerSurface: Surface? = null

    val textureView = TextureView(context).apply {
        this@VideoItemView.addView(this)
    }

    private var videoUri: Uri? = null

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
        val matchMeasureWidth =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)
        val matchMeasureHeight =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)
        textureView.measure(matchMeasureWidth, matchMeasureHeight)
        cover.measure(matchMeasureWidth, matchMeasureHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        textureView.layout(0, 0, width, height)
        cover.layout(0, 0, width, height)
    }

    fun setVideoUri(videoUri: Uri) {
        this.videoUri = videoUri
    }

    fun play(videoUri: Uri) {
        setVideoUri(videoUri)
        if (player == null)
            VideoPlayCounter.play()
        stop()
        player = if (VideoPlayerManager.selectedPlayer == VideoPlayerManager.PLAYER_EXO) {
            Log.i(TAG, "player: EXO")
            SimpleExoMediaPlayer(context)
        } else {
            Log.i(TAG, "player: IJK")
            IjkMediaPlayer()
        }
        player?.setDataSource(context, videoUri)
        player?.isLooping = true
        if (playerSurface != null && playerSurface?.isValid == true) {
            player?.setSurface(playerSurface)
            player?.prepareAsync()
            Log.i(TAG, "play: prepare")
        } else {
            val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    playerSurface?.release()
                    playerSurface = Surface(surface)
                    player?.setSurface(playerSurface)
                    if (player?.isPlaying == false) {
                        player?.prepareAsync()
                        Log.i(TAG, "onSurfaceTextureAvailable: ")
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    Log.i(TAG, "onSurfaceTextureDestroyed: ")
                    VideoPlayerManager.add(this@VideoItemView)
                    playerSurface?.release()
                    surface.release()
                    playerSurface = null
                    textureView.surfaceTextureListener = null
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                }

            }
            textureView.surfaceTextureListener = surfaceTextureListener
        }
        textureView.visible()
    }

    fun stop() {
        if (player != null)
            VideoPlayCounter.stop()
        player?.stop()
        player = null
        textureView.invisible()
    }

    fun restart() {
        if (player != null) {
            return
        }
        videoUri?.let {
            play(it)
        }
    }

    override fun onPlayingCallback(isPlaying: Boolean) {
        if (isAttachedToWindow)
            if (isPlaying) {
                textureView.visible()
            } else {
                textureView.invisible()
            }
    }

    companion object {
        private const val TAG = "VideoItemView"
    }
}