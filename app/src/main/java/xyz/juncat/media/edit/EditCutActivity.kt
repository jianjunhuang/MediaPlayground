package xyz.juncat.media.edit

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.juncat.media.SimplePlayer
import xyz.juncat.media.base.Utils
import xyz.juncat.media.databinding.ActivityMp4EditBinding
import java.io.File

class EditCutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMp4EditBinding

    private var videoUri: Uri? = null
    private val player by lazy {
        SimplePlayer.Builder(this)
            .registerLifecycle(lifecycle)
            .setVideoUri(videoUri)
            .setVideoView(binding.preVideoView)
            .build()
    }

    private var resultPlayer: SimplePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMp4EditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoUri = intent.data
        videoUri?.let {
            player.play()
        }
        binding.btnCut.setOnClickListener {
            cut()
        }
        binding.btnFfmpegCut.setOnClickListener {
            cutFFmpeg()
        }
        binding.btnFfmpegCrop.setOnClickListener {
            cropFFmpeg()
        }
    }

    private fun cropFFmpeg() {
        lifecycleScope.launch(Dispatchers.IO) {
            log("copying file")
            val file = Utils.copyInternal(this@EditCutActivity, videoUri!!)
            val outputFile = File(externalCacheDir, "cut_file.mp4")
            if (outputFile.exists()) outputFile.delete()
            log("local file -> $file")
            val information = FFprobeKit.getMediaInformation(file.absolutePath).mediaInformation
            Log.i("EditCutActivity", information.allProperties.toString())
            val jsonObject = information.allProperties
            val attr = jsonObject.optJSONArray("streams").getJSONObject(0)
            val width = attr.getInt("width")
            val height = attr.getInt("height")
            val duration = attr.getDouble("duration")
            log("width: $width, height: $height, duration: $duration")
            val startTime = System.currentTimeMillis()
            val cmd =
                "-i ${file.absolutePath} -filter:v \"crop=${width / 2}:${height / 2}:0:0\" -c:a copy ${outputFile.absolutePath}"
            log("cmd: $cmd")
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                log("success -> ${System.currentTimeMillis() - startTime}")
                withContext(Dispatchers.Main) {
                    if (resultPlayer != null) {
                        resultPlayer?.stop()
                    }
                    resultPlayer = SimplePlayer.Builder(this@EditCutActivity)
                        .registerLifecycle(lifecycle)
                        .setVideoUri(outputFile.toUri())
                        .setVideoView(binding.resultVideoView)
                        .build()
                    resultPlayer?.play()
                }
                val newDuration =
                    FFprobeKit.getMediaInformation(outputFile.absolutePath).mediaInformation.duration.toFloat()
                log("new duration: $newDuration")
            } else {
                log("failed")
                session.allLogs.forEach {
                    log("${it.level}: ${it.message}")
                }
            }
        }
    }

    private fun cutFFmpeg() {
        lifecycleScope.launch(Dispatchers.IO) {
            log("copying file")
            val file = Utils.copyInternal(this@EditCutActivity, videoUri!!)
            val parent = File(externalCacheDir, "cut")
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val outputFile = File(parent, "cut_file.ts")
            val outputTSPath = File(parent, "pending.ts")
            if (outputFile.exists()) outputFile.delete()
            log("local file -> $file")
            val duration =
                FFprobeKit.getMediaInformation(file.absolutePath).mediaInformation.duration.toFloat()
            log("video duration -> ${duration}")
            val startTime = System.currentTimeMillis()
            val start = 0
            val end = 5
            val cmd = "-i ${file.absolutePath} -ss $start -t $end ${outputFile.absolutePath}"
            log("cmd: $cmd")
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                log("success -> ${System.currentTimeMillis() - startTime}")
                withContext(Dispatchers.Main) {
                    if (resultPlayer != null) {
                        resultPlayer?.stop()
                    }
                    resultPlayer = SimplePlayer.Builder(this@EditCutActivity)
                        .registerLifecycle(lifecycle)
                        .setVideoUri(outputFile.toUri())
                        .setVideoView(binding.resultVideoView)
                        .build()
                    resultPlayer?.play()
                }
                val newDuration =
                    FFprobeKit.getMediaInformation(outputFile.absolutePath).mediaInformation.duration.toFloat()
                log("new duration: $newDuration")
            } else {
                log("failed")
                session.allLogs.forEach {
                    log("${it.level}: ${it.message}")
                }
            }
        }
    }

    private fun cut() {
        lifecycleScope.launch(Dispatchers.IO) {
            log("copying file")
            val file = Utils.copyInternal(this@EditCutActivity, videoUri!!)
            val outputFile = File(externalCacheDir, "cut_file.mp4")
            if (outputFile.exists()) outputFile.delete()
            log("local file -> $file")
            val duration = Utils.getVideoDuration(file.absolutePath)
            log("video duration -> ${duration}")
            val start = System.currentTimeMillis()
            log("cut 0 to 5s")
            VideoUtils.startTrim(
                file.absolutePath,
                outputFile.absolutePath,
                0,
                5000,
                true,
                true,
                object : VideoUtils.Listener {
                    override fun onStart() {
                        log("start cut")
                    }

                    override fun onProgress(value: Float) {
                    }

                    override fun onComplete() {
                        log("complete cut: ${System.currentTimeMillis() - start}")
                        log("video duration -> ${Utils.getVideoDuration(outputFile.absolutePath)}")
                        if (resultPlayer != null) {
                            resultPlayer?.stop()
                        }
                        resultPlayer = SimplePlayer.Builder(this@EditCutActivity)
                            .registerLifecycle(lifecycle)
                            .setVideoUri(outputFile.toUri())
                            .setVideoView(binding.resultVideoView)
                            .build()
                        resultPlayer?.play()
                    }

                    override fun onError(message: String) {
                        log("cut error :$message")
                    }

                })
        }
    }

    private fun log(str: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvLog.append(str)
            binding.tvLog.append("\n")
        }
    }
}