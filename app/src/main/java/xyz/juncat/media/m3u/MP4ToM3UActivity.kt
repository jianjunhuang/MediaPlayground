package xyz.juncat.media.m3u

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.juncat.media.SimplePlayer
import xyz.juncat.media.databinding.ActivityMp4ToM3u8Binding
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class MP4ToM3UActivity : AppCompatActivity() {

    private var videoUri: Uri? = null
    private lateinit var binding: ActivityMp4ToM3u8Binding
    private val player by lazy {
        SimplePlayer.Builder(this)
            .registerLifecycle(lifecycle)
            .setVideoUri(videoUri)
            .setVideoView(binding.videoView)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMp4ToM3u8Binding.inflate(layoutInflater)
        setContentView(binding.root)
        videoUri = intent.data
        videoUri?.let {
            player.play()
        }
        binding.btnCover.setOnClickListener {
            videoUri?.let {
                convert(it)
            }
        }
        binding.btnCoverFast.setOnClickListener {
            videoUri?.let {
                convertFaster(it)
            }
        }
        binding.btnM3u2Mp4.setOnClickListener {
            convertToMp4()
        }
    }

    private fun convertToMp4() {
        val m3uFolder = File(externalCacheDir?.absolutePath, "m3u8_fast")
        val m3uPath = m3uFolder.absolutePath + "/test.m3u8"

        lifecycleScope.launch(Dispatchers.Default) {
            val m3uFile = File(m3uPath)
            if (!m3uFile.exists()) {
                return@launch
            }
            val mp4Out = m3uFolder.absolutePath + "/out.mp4"
            val out = File(mp4Out)
            if (out.exists()) {
                out.delete()
            }
            val cmd = "-protocol_whitelist file,http,https,tcp,tls,crypto -i $m3uPath -vcodec copy -acodec copy -absf aac_adtstoasc $mp4Out"
            appendText(cmd)
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                appendText("\nsuccess")
            } else {
                session.allLogs.forEach {
                    appendText("${it.level} :${it.message}\n")
                }
            }
        }
    }

    private fun convert(uri: Uri) {
        binding.tvContent.text = ""
        lifecycleScope.launch(Dispatchers.Default) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(this@MP4ToM3UActivity, uri)
            val internalFile = File(externalCacheDir, "${System.currentTimeMillis()}.mp4")
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
            val outputFolder = File(externalCacheDir?.absolutePath, "m3u8")
            outputFolder.deleteRecursively()
            outputFolder.mkdir()
            val outputPath = outputFolder.absolutePath + "/output.m3u8"
            val cmd =
                "-i ${internalFile.absolutePath} -b:v 1024k -g 60 -hls_time 10 -hls_list_size 0 -hls_segment_size 500000 $outputPath"
            appendText("cmd: $cmd \n")
            //show cmd
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                //show success
                appendText("success!!\n")
                appendText("Spend Time: ${(System.currentTimeMillis() - start) / 1000f}s\n")
                appendText("list file:\n")
                outputFolder.listFiles()?.forEach {
                    appendText(" - ${it.name}\n")
                }
                val ins = BufferedReader(InputStreamReader(FileInputStream(outputPath)))
                appendText("\n=== ${internalFile.name}.m3u8 ===\n")
                appendText(ins.readText())
                ins.close()
            } else {
                //show error
                session.allLogs.forEach {
                    appendText("${it.level} :${it.message}\n")
                }
            }
            internalFile.delete()
        }
    }

    private fun convertFaster(uri: Uri) {
        binding.tvContent.text = ""
        lifecycleScope.launch(Dispatchers.Default) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(this@MP4ToM3UActivity, uri)
            val internalFile = File(externalCacheDir, "${System.currentTimeMillis()}.mp4")
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
            val outputFolder = File(externalCacheDir?.absolutePath, "m3u8_fast")
            outputFolder.deleteRecursively()
            outputFolder.mkdir()
            val outputTSPath = outputFolder.absolutePath + "/temp.ts"
            val outputCombineTSPath = outputFolder.absolutePath + "/all.ts"
            val outputPath = outputFolder.absolutePath + "/output.m3u8"
            val toTSCMD =
                "-y -i ${internalFile.absolutePath} -vcodec copy -acodec copy -vbsf h264_mp4toannexb $outputTSPath"
            val combineCMD =
                "-i \"concat:$outputTSPath|$outputTSPath\" -c copy $outputCombineTSPath"
            val cmd =
                "-i $outputCombineTSPath -c copy -map 0 -f segment -segment_list $outputPath -segment_time 10 ${outputFolder.absolutePath}/15s%3d.ts"
            appendFast(">>> ts cmd: $toTSCMD \n")
            val tsSession = FFmpegKit.execute(toTSCMD)
            tsSession.cancel()
            if (tsSession.returnCode.isValueSuccess) {
                appendFast("to ts success!!\n")
                appendFast("to ts Spend Time: ${(System.currentTimeMillis() - start) / 1000f}s\n")

                appendFast("combine two ts\n")
                val combineSession = FFmpegKit.execute(combineCMD)
                if (!combineSession.returnCode.isValueSuccess) {
                    combineSession.allLogs.forEach {
                        appendFast("${it.level} :${it.message}\n")
                    }
                    return@launch
                }

                appendFast(">>> m3u8 cmd: $cmd\n")
                val session = FFmpegKit.execute(cmd)
                if (session.returnCode.isValueSuccess) {
                    //show success
                    appendFast("success!!\n")
                    appendFast("Spend Time: ${(System.currentTimeMillis() - start) / 1000f}s\n")
                    appendFast("list file:\n")
                    outputFolder.listFiles()?.forEach {
                        appendFast(" - ${it.name}\n")
                    }
                    val ins = BufferedReader(InputStreamReader(FileInputStream(outputPath)))
                    appendFast("\n=== ${internalFile.name}.m3u8 ===\n")
                    appendFast(ins.readText())
                    ins.close()
                } else {
                    //show error
                    session.allLogs.forEach {
                        appendFast("${it.level} :${it.message}\n")
                    }
                }
            } else {
                tsSession.allLogs.forEach {
                    appendFast("${it.level} :${it.message}\n")
                }
            }

            internalFile.delete()
        }
    }

    private suspend fun appendText(text: String) = withContext(Dispatchers.Main) {
        binding.tvContent.append(text)
    }

    private suspend fun appendFast(text: String) = withContext(Dispatchers.Main) {
        binding.tvFastContent.append(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.onDestroy()
    }
}