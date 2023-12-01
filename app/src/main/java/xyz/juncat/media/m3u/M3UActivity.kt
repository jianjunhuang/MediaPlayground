package xyz.juncat.media.m3u

import android.net.Uri
import android.os.Bundle
import android.view.TextureView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import xyz.juncat.media.SimplePlayer

class M3UActivity : AppCompatActivity() {

    private var videoUri: Uri? = null
    private lateinit var textureView: TextureView
    private val player by lazy {
        SimplePlayer.Builder(this)
            .registerLifecycle(lifecycle)
            .setVideoUri(videoUri)
            .setVideoView(textureView)
            .build()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textureView = TextureView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(textureView)
        videoUri = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8".toUri()

        player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.onDestroy()
    }
}