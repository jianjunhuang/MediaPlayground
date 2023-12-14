package xyz.juncat.media.ijk

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import xyz.juncat.media.databinding.ActivityIjkPlayerBinding

class IjkPlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIjkPlayerBinding

    private var ijkPlayer: IjkMediaPlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playerSurface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIjkPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnIjk.setOnClickListener {
            changePlayer(Player.IJK)
        }
        binding.btnMediaPlay.setOnClickListener {
            changePlayer(Player.MEDIA)
        }
        binding.videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

                playerSurface = Surface(surface)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                ijkPlayer?.release()
                mediaPlayer?.release()
                playerSurface?.release()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    private fun changePlayer(player: Player) {
        ijkPlayer?.stop()
        ijkPlayer?.release()

        mediaPlayer?.stop()
        mediaPlayer?.release()
        intent.data?.let {
            if (player == Player.IJK) {
                ijkPlayer = IjkMediaPlayer()
                ijkPlayer?.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
                ijkPlayer?.setDataSource(this@IjkPlayActivity, it)
                ijkPlayer?.setSurface(playerSurface)
                ijkPlayer?.isLooping = true
                ijkPlayer?.prepareAsync()
            } else {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(this@IjkPlayActivity, it)
                mediaPlayer?.setSurface(playerSurface)
                mediaPlayer?.isLooping = true
                mediaPlayer?.prepare()
                mediaPlayer?.start()
            }
        }
    }

    private enum class Player {
        IJK, MEDIA
    }
}