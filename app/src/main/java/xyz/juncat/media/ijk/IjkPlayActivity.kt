package xyz.juncat.media.ijk

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import xyz.juncat.media.databinding.ActivityIjkPlayerBinding
import java.io.File

class IjkPlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIjkPlayerBinding
    private var player: IMediaPlayer? = null
    private var playerSurface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIjkPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

                val outputFolder = File(externalCacheDir?.absolutePath, "m3u8_fast")
                val outputPath = outputFolder.absolutePath + "/output.m3u8"
                intent.data?.let {
                    player = IjkMediaPlayer()
                    player?.setDataSource(this@IjkPlayActivity, outputPath.toUri())
                    playerSurface = Surface(surface)
                    player?.setSurface(playerSurface)
                    player?.isLooping = true
                    player?.prepareAsync()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                player?.release()
                playerSurface?.release()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

}