package xyz.juncat.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.juncat.media.databinding.ActivityMusicExtractorBinding
import java.io.File
import java.nio.ByteBuffer

class MusicExtractorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMusicExtractorBinding
    private var videoUri: Uri? = null

    private val player by lazy {
        SimplePlayer.Builder(this)
            .registerLifecycle(lifecycle)
            .setVideoUri(videoUri)
            .setVideoView(binding.videoView)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicExtractorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoUri = intent.data
        videoUri?.let {
            startPlayVideo()
        }

        binding.btnExtract.setOnClickListener {
            videoUri?.let {
                extract(it)
            }
        }
    }

    private fun startPlayVideo() {
        player.play()
    }

    /**
     * 获取到目标数据流后，不能直接输出，需要通过 MediaMuxer 输出。
     */
    private fun extract(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            val extractor = MediaExtractor()
            var audioTrackIndex = 0
            val tmpAudioFile =
                File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${System.currentTimeMillis()}.mp3")
            val muxer = MediaMuxer(tmpAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            extractor.setDataSource(this@MusicExtractorActivity, uri, null)
            val numTracks = extractor.trackCount
            for (index in 0 until numTracks) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME)
                Log.i(TAG, "extract: $mime")
                if (mime?.startsWith("audio") == true) {
                    audioTrackIndex = muxer.addTrack(format)
                    extractor.selectTrack(index)
                    break
                }
            }
            val inputBuffer = ByteBuffer.allocate(1024)
            val bufferInfo = MediaCodec.BufferInfo()
            muxer.start()
            var size = 0
            while (extractor.readSampleData(inputBuffer, 0).apply { size = this } > 0) {
                bufferInfo.size = size
                bufferInfo.offset = 0
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(audioTrackIndex, inputBuffer, bufferInfo)
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            extractor.release()
            //直接播放生成的音频
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MusicExtractorActivity, "finish! and play music", Toast.LENGTH_SHORT).show()
                player.stop()

                SimplePlayer.Builder(this@MusicExtractorActivity)
                    .registerLifecycle(lifecycle)
                    .setVideoUri(tmpAudioFile.toUri())
                    .setVideoView(binding.videoView)
                    .build()
                    .play()
            }
        }
    }

    companion object {
        private const val TAG = "MusicExtractorActivity"
    }
}