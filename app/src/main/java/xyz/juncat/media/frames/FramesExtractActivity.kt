package xyz.juncat.media.frames

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.juncat.media.SimplePlayer
import xyz.juncat.media.databinding.ActivityFramesExtractorBinding
import java.io.File
import java.io.FileOutputStream


class FramesExtractActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFramesExtractorBinding
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
        binding = ActivityFramesExtractorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoUri = intent.data
        videoUri?.let {
            player.play()
            getVideoInfo(it)
        }
        binding.btnFfmpeg.setOnClickListener {
            videoUri?.let {
                extractByFFmpeg(it)
            }
        }
        binding.btnRetriever.setOnClickListener {
            videoUri?.let {
                extractByRetriever(it)
            }
        }
        binding.btnMediacodec.setOnClickListener {
            videoUri?.let {
                extractByMediaCodec(it)
            }
        }
    }

    private fun extractCount() = binding.editCount.text?.toString()?.toIntOrNull() ?: 5

    private fun extractByRetriever(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            start("retriever")
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this@FramesExtractActivity, uri)
            val durationMillis =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: 0
            val durationUs = durationMillis * 1_000
            val offset = durationUs / extractCount()
            val frames = ArrayList<Bitmap>()
            val count = extractCount()
            val dstWidth = binding.framesView.width / count
            val dstHeight = binding.framesView.height
            for (time in 0..durationUs step offset) {
                retriever.getFrameAtTime(time)?.let {
                    frames.add(it)
                }
//                retriever.getScaledFrameAtTime(
//                    time, MediaMetadataRetriever.OPTION_CLOSEST, dstWidth, dstHeight
//                )?.let {
//                    frames.add(it)
//                }
            }
            setCostTimeText("retriever", System.currentTimeMillis() - start)
            binding.framesView.setFrames(frames)
        }
    }

    private fun getVideoInfo(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(this@FramesExtractActivity, uri)
            val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
            val info = mediaInformation.mediaInformation
            withContext(Dispatchers.Main) {
                binding.tvInfo.text = """
                    format = ${info.format}
                    duration = ${info.duration}
                    size = ${info.size}
                    bitrate = ${info.bitrate}
                """.trimIndent()
            }
        }

    }

    private fun extractByFFmpeg(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(this@FramesExtractActivity, uri)
            val internalFile = File(externalCacheDir, System.currentTimeMillis().toString())
            //copy to internal ,case image2 can not handle SAF uri
            //https://github.com/tanersener/ffmpeg-kit/issues/39
            if (!internalFile.exists()) {
                val ins = contentResolver.openInputStream(uri)
                val ops = FileOutputStream(internalFile)
                val byteArray = ByteArray(1024)
                var len = 0
                while (ins?.read(byteArray)?.let {
                        len = it
                        len > 0
                    } == true
                ) {
                    ops.write(
                        byteArray, 0, len
                    )
                }
                ops.flush()
            }

            val start = System.currentTimeMillis()
            start("ffmpeg")
            val duration =
                FFprobeKit.getMediaInformation(inputPath).mediaInformation.duration.toFloat()
            val count = extractCount()
            val fps = count.toFloat() / duration
            val dstWidth = binding.framesView.width / count
            val dstHeight = binding.framesView.height
            val outputFolder = File(externalCacheDir?.absolutePath, "img_start")
            if (!outputFolder.exists())
                outputFolder.mkdir()
            val outputPath = outputFolder.absolutePath + "/%3d.jpg"
            val cmd =
                "-i ${internalFile.absolutePath} -f image2 -vf fps=$fps -s ${dstWidth}x${dstHeight} -ss 0 -t $duration -q:v 10 $outputPath"
            Log.i(TAG, "extractByFFmpeg: $cmd")
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                Log.i(TAG, "extractByFFmpeg: extract file ${System.currentTimeMillis() - start}")
                val list = ArrayList<Bitmap>()
                outputFolder.listFiles()?.forEach {
                    val bmp = BitmapFactory.decodeFile(it.absolutePath)
                    list.add(bmp)
                }
                binding.framesView.setFrames(list)
            }
            outputFolder.delete()
            setCostTimeText("ffmpeg", System.currentTimeMillis() - start)
        }
    }

    private fun extractByMediaCodec(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            val extractor = ExtractMpegFrames()
            val start = System.currentTimeMillis()
            start("MediaCodec")
            val outputFolder = File(externalCacheDir?.absolutePath, "img_codec")
            if (!outputFolder.exists()) {
                outputFolder.mkdir()
            }
            val count = extractCount()
            val dstWidth = binding.framesView.width / count
            val dstHeight = binding.framesView.height
            extractor.extractMpegFrames(
                this@FramesExtractActivity,
                uri,
                outputFolder,
                dstWidth,
                dstHeight
            )
            setCostTimeText("MediaCodec", System.currentTimeMillis() - start)
            val list = ArrayList<Bitmap>()
            outputFolder.listFiles()?.forEach {
                val bmp = BitmapFactory.decodeFile(it.absolutePath)
                list.add(bmp)
            }
            binding.framesView.setFrames(list)
            outputFolder.delete()
        }
    }

    private suspend fun setCostTimeText(from: String, cost: Long) {
        withContext(Dispatchers.Main) {
            binding.tvTime.text = "$from -> cost ${cost} ms"
        }
    }

    private suspend fun start(from: String) {
        withContext(Dispatchers.Main) {
            binding.tvTime.text = "$from"
        }
    }

    companion object {
        private const val TAG = "FramesExtractActivity"
    }
}