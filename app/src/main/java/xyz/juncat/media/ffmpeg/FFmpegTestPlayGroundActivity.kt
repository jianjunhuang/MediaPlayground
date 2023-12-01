package xyz.juncat.media.ffmpeg

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import xyz.juncat.media.base.LogActivity
import xyz.juncat.media.base.Utils
import java.io.File
import java.io.FileWriter
import kotlin.coroutines.resume

class FFmpegTestPlayGroundActivity : LogActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initActionView(frameLayout: FrameLayout) {
        frameLayout.addActionButton(object : OnClickListener {
            override fun onClick(v: View?) {
                lifecycleScope.launch(Dispatchers.IO) {
                    testOnline()
                }
            }
        })
    }

    private suspend fun test() {
        val cmd = "-h"
        runAsync(cmd)
    }

    /**
     * 查看 color format 兼容情况
     * 也就是 pix_fmt
     */
    private suspend fun checkColorFormat() {
        val pendingColors = mapOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface to "COLOR_FormatSurface",
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible to "COLOR_FormatYUV420Flexible",
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar to "COLOR_FormatYUV420PackedPlanar",
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar to "COLOR_FormatYUV420PackedSemiPlanar",
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar to "COLOR_FormatYUV420Planar",
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar to "COLOR_FormatYUV420SemiPlanar",
            MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar to "COLOR_QCOM_FormatYUV420SemiPlanar",
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar to "COLOR_TI_FormatYUV420PackedSemiPlanar",
        )
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .filter { it.isEncoder }
            .filter { it.supportedTypes.contains("video/avc") }
            .forEach {
                log("\ncodec: ${it.name}, is software: ")
                it.getCapabilitiesForType("video/avc").colorFormats.forEach {
                    log("  color:$it ${pendingColors.getOrElse(it) { "not support" }}")
                }
            }

        try {
            val format = MediaFormat.createVideoFormat("video/avc", 1920, 1080)
            val codec = MediaCodec.createEncoderByType("video/avc")
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 24)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            codec.configure(
                format,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            )
            log("success")
        } catch (t: Throwable) {
            log(t.stackTraceToString())
        }
    }

    private suspend fun selectVideo() {
        val uri = selectMedia("video/*")
        if (uri != null) {
            val path = FFmpegKitConfig.getSafParameterForRead(
                this@FFmpegTestPlayGroundActivity,
                uri
            )
            testMediacodec(path)
        } else {
            log("uri is null", true)
        }
    }

    private suspend fun selectEncoder(): String? = suspendCancellableCoroutine { con ->
        AlertDialog.Builder(this)
            .setTitle("Select encoder")
            .setSingleChoiceItems(
                arrayOf("libopenh264", "h264_mediacodec"),
                1
            ) { dialog, which ->
                dialog.dismiss()
                con.resume(
                    when (which) {
                        0 -> {
                            "libopenh264"
                        }

                        else -> {
                            "h264_mediacodec"
                        }
                    }
                )

            }.setOnCancelListener {
                con.resume(null)
            }
            .show()
    }

    private suspend fun showAllPixFmt() {
        val cmd = "-h encoder=h264_mediacodec"
        runAsync(cmd)
    }

    /**
     * 显示所有编码器
     */
    private suspend fun showAllMeidaCodec() {
        log(
            "codec => " + MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(
                MediaFormat.createVideoFormat(
                    "video/avc",
                    1920,
                    1080
                )
            )
        )
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.forEach { info ->
            if (info.isEncoder) {
                log(
                    """  
            CodecInfo:====>                
                name: ${info.name}  
                softwareOnly: ${info.isSoftwareOnly}  
                isVendor: ${info.isVendor}  
        """.trimIndent()
                )

                info.supportedTypes.forEach {
                    info.getCapabilitiesForType(it).colorFormats.forEach {
                        log("colorFormat: $it")
                    }
                    val videoCap = info.getCapabilitiesForType(it).videoCapabilities
                    log(
                        """  
                    type: $it  
                        supportWidth: ${videoCap?.supportedHeights}  
                        supportHeight: ${videoCap?.supportedWidths}  
        """.trimIndent()
                    )
                }
            }
        }
    }

    private suspend fun testMediacodec(path: String) {
        val folder = File(externalCacheDir, "ffmpeg")
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        folder.mkdir()
        val encoder = withContext(Dispatchers.Main) {
            selectEncoder()
        }
        log("=============== : $encoder :================\n")
        val codecName = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(
            MediaFormat.createVideoFormat(
                "video/avc",
                1920,
                1080
            )
        )
        log("codecName: $codecName")
        val cmd =
            "-i $path -c:v $encoder -codec_name $codecName -bitrate_mode 1 -level 4 -r 24 -pix_fmt nv12 -b:v 64k -bufsize 64k -c:a copy -y ${folder.absolutePath}/mediacodec.mp4"
        run(cmd)
    }

    private suspend fun testCmd() {
        val pkg = "xyz.juncat.videoeditor"

        val folder = File(externalCacheDir, "ffmpeg")
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        folder.mkdir()
        (0..10).forEach { index ->
            val endGOPFile = File(folder, "${index}_gop_end.ts")
            val duration = 8.510367 - (index * 0.01)
            val cutTime = (index * 0.01)
            val cmd =
                "-i /storage/emulated/0/Android/data/$pkg/files/500447191346655232/draft/XR5gzydE/media/clipVideo/gop_-1a5efb1.ts -ss $duration -t $cutTime -c:v libopenh264 -profile:v high -c:a copy ${endGOPFile.path}"

            log("cutTime: $cutTime\n")
            log("cmd: $cmd\n")
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) {
                log("success: ${session.allLogsAsString}")
            } else {
                log("error: ${session.returnCode}", true)
                log(session.allLogsAsString, true)
            }
        }
    }

    private suspend fun testAACResample() {
        val pkg = "xyz.juncat.videoeditor"
        val cmd =
            "-i /storage/emulated/0/Android/data/$pkg/cache/ext_audio/ext.aac -ac 1 -ar 16000 -y /storage/emulated/0/Android/data/$pkg/cache/ext_audio/ext_ssrc.aac"
        val start = System.currentTimeMillis()
        val session = FFmpegKit.execute(cmd)
        log("cost: ${System.currentTimeMillis() - start}")

        if (session.returnCode.isValueSuccess) {
            log("success: ${session.allLogsAsString}")
        } else {
            log("error: ${session.returnCode}", true)
            log(session.allLogsAsString, true)
        }

    }

    private suspend fun testGetGOP() {
        val sourcePath = File(externalCacheDir, "hm1ir")
        val source = File(sourcePath, "hm1ir.mp4")
        val output = File(sourcePath, "gop.ts")
        val cmd =
            "-ss 0.046 -i ${source.absoluteFile} -c copy -y ${output.absoluteFile}"
        runAsync(cmd)

        val output1 = File(sourcePath, "gop1.ts")
        val cmd1 =
            "-ss 0.046 -i ${source.absoluteFile} -c copy -y ${output1.absoluteFile}"
        runAsync(cmd1)
    }

    private suspend fun testCut() {
        val sourcePath = File(externalCacheDir, "hm1ir")
        val source = File(sourcePath, "hm1ir.mp4")
        val output = File(sourcePath, "224410db.mp4")
        val cmd =
            "-ss 0 -i ${source.absoluteFile} -t 1.963 -c copy -y ${output.absoluteFile}"
        runAsync(cmd)
    }

    /**
     * https://stackoverflow.com/questions/18064604/frame-rate-very-high-for-a-muxer-not-efficiently-supporting-it
     */
    private suspend fun testDecodeGOP() {
        val sourcePath = File(externalCacheDir, "mykh9")
        val source = File(sourcePath, "gop_38dcac02.ts")
        val output = File(sourcePath, "-76995e02.mp4")
        val cmd =
            "-i ${source.absoluteFile} -ss 4.118 -t 3.844 -c:v libopenh264 -profile:v high -vsync 2 -c:a copy  -y ${output.absoluteFile}"
        runAsync(cmd)
    }

    private suspend fun runAsync(cmd: String) {
        val start = System.currentTimeMillis()
        FFmpegKit.executeAsync(cmd) { session ->
            CoroutineScope(Dispatchers.IO).launch {

                log("cmd: $cmd")
                log("cost: ${System.currentTimeMillis() - start}")

                if (session.returnCode.isValueSuccess) {
                    log("success: ${session.allLogsAsString}")
                } else {
                    log("error: ${session.returnCode}", true)
                    log(session.allLogsAsString, true)
                }
            }
        }

    }

    private suspend fun run(cmd: String, log: Boolean = true): Long {
        val start = System.currentTimeMillis()
        log("running cmd: $cmd")
        val session = FFmpegKit.execute(cmd)
        val cost = System.currentTimeMillis() - start
        log("cost: $cost")

        if (session.returnCode.isValueSuccess) {
            if (log) log("success: ${session.allLogsAsString}")
        } else {
            log("error: ${session.returnCode}", true)
            log(session.allLogsAsString, true)
        }
        return cost
    }


    private suspend fun testH264Encoder() {
        val uri = selectMedia("video/*")
        if (uri == null) {
            log("uri is null", true)
            return
        }
        val srcPath = Utils.copyInternal(this, uri, "ffmpeg")
        val parent = srcPath.parentFile
        // test change rate
//        log("================= change rate =================")
//        log("================= MEDIACODEC =================")
//        run(
//            "-i ${srcPath.absolutePath} -c:v h264_mediacodec -r 24 -pix_fmt nv12 -c:a copy -y ${parent.absolutePath}/mediacodec_24.mp4",
//            false
//        )
//        log("================= libopenh264 =================")
//        run(
//            "-i ${srcPath.absolutePath} -c:v libopenh264 -r 24 -pix_fmt nv12 -c:a copy -y ${parent.absolutePath}/libopenh264_24.mp4",
//            false
//        )
//
//        log("\n================= cut =================")
        log("================= MEDIACODEC =================")
//        run(
//            "-i ${srcPath.absolutePath} - y ${parent.absolutePath}/mediacodec_ts.ts"
//        )
        run(
            "-i ${parent.absolutePath}/mediacodec_ts.ts -ss 10.001 -t 10.888 -c:v h264_mediacodec -pix_fmt nv12 -c:a copy -y ${parent.absolutePath}/mediacodec_cut.ts"
        )
//        log("================= libopenh264 =================")
//        run(
//            "-i ${srcPath.absolutePath} -ss 10 -t 10 -c:v libopenh264 -pix_fmt nv12 -y ${parent.absolutePath}/libopenh264_cut.mp4",
//            false
//        )

//        log("\n================= ass =================")
//        log("================= MEDIACODEC =================")
//        run(
//            "-i ${srcPath.absolutePath} -vf subtitles=/sdcard/Android/data/xyz.juncat.videoeditor/cache/ffmpeg/subtitles.ass -c:v h264_mediacodec -pix_fmt nv12 -y ${parent.absolutePath}/mediacodec_ass.mp4",
//            false
//        )
//        log("================= libopenh264 =================")
//        run(
//            "-i ${srcPath.absolutePath} -vf subtitles=/sdcard/Android/data/xyz.juncat.videoeditor/cache/ffmpeg/subtitles.ass -c:v libopenh264 -pix_fmt nv12 -y ${parent.absolutePath}/libopenh264_ass.mp4",
//            false
//        )
    }

    private suspend fun testGetIFrame() {
        val cmd =
            "-loglevel error -select_streams v:0 -show_entries packet=pts_time,flags -of json -i /storage/emulated/0/Android/data/xyz.juncat.videoeditor/cache/ffmpeg/xSD5H.mp4"
        val start = System.currentTimeMillis()
        log("running cmd: $cmd")
        val session = FFprobeKit.execute(cmd)
        val cost = System.currentTimeMillis() - start
        log("cost: $cost")

        if (session.returnCode.isValueSuccess) {
            log("success: ${session.allLogsAsString}")
        } else {
            log("error: ${session.returnCode}", true)
            log(session.allLogsAsString, true)
        }
    }

    private suspend fun testM3U8Mp4() {
        val file = File(externalCacheDir, "test")
        run("-ss 10.882133 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 10.8718 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-153e4a1d.ts")
        run("-ss 21.753922 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 10.842 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_2de90dcc.ts")
        run("-ss 21.753922 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 10.842 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_2de90dcc.ts")
        run("-ss 0 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 4.03 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/-437e27a7.mp4")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-153e4a1d.ts -ss 7.9079 -t 0.13 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/2ca0e95d.mp4")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_2de90dcc.ts -ss 1.4261 -t 0.33 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/2a9875d4.mp4")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_2de90dcc.ts -ss 5.0651 -t 5.7769 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/decode_-903c1ee.ts")
        run("-ss 43.8514 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 10.8664 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-3ddae0d2.ts")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-3ddae0d2.ts -ss 4.5886 -t 6.2778 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/decode_-2e081ee2.ts")
        run("-ss 65.471467 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 11.2552 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_3800c4d2.ts")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_3800c4d2.ts -ss 8.9385 -t 2.3167 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/decode_-6be37966.ts")
        run("-ss 109.803778 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 10.8881 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-27d631ed.ts")
        run("-ss 32.595889 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 0.2041 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/copy_-197d758a.ts")
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-27d631ed.ts -ss 0.0962 -t 7.17 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/686324.mp4")
        run("-f concat -safe 0 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/cpR7N.txt -c copy ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/-23e9fb79.mp4")//todo
        run("-ss 54.7178 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 11.7422 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/copy_-64927af7.ts")
        run("-ss 76.7267 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -t 13.4333 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/copy_20dc36ea.ts")
        run("-f concat -safe 0 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/ZaymR.txt -c copy ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/16829d8b.mp4")//todo
        run("-ss 131.830333 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/record/igQ--.mp4 -c copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-1d53d11e.ts")
        run("-f concat -safe 0 -i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/FM1QC.txt -c copy ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/-62d52ad0.mp4")//todo
        run("-i ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/gop_-1d53d11e.ts -ss 0.9197 -t 0.5 -c:v libopenh264 -vsync 2 -c:a copy -y ${file.path}/capsuleClient/draft/n1s38qGQ/media/clipVideo/-6729671.mp4")

        run("-f concat -safe 0 -i ${file.path}/capsuleClient/shared/export/2QCCj.m3u8/files.txt -hls_list_size 0 -hls_time 6 -f hls -c copy ${file.path}/capsuleClient/shared/export/2QCCj.m3u8/-52e3f12.m3u8")

    }

    private suspend fun testM3U8() {
        testProbe()
    }

    private var count = 0
    private suspend fun testProbe() {
        val file = File(externalCacheDir, "m3u8")
        count++
//        ffprobeAsync(
//            "-loglevel error -select_streams v:0 -show_entries packet=pts_time,flags -of json -i ${file}/origin30min.mp4 -select \"flags=eq()\"",
//            false
//        ) {
//            lifecycleScope.launch(Dispatchers.IO) {
//                if (count < 11) {
//                    testProbe()
//                }
//            }
//        }
    }


    private suspend fun testOnline() {
        val file = externalCacheDir

//   run("-i ${file?.path}/gop_-6eabaa1b.ts -ss 2.38 -t 5.678 -c:v libopenh264 -b:v 8000k -minrate 8000k -maxrate 8000k -bf 0 -profile:v high -vsync 2 -c:a copy -y ${file?.path}/426a1c45_tmp.ts")
        run("-ss 337.647411 -i ${file}/draft/PvVlagKo/media/concatMp4/concat.mp4 -t 0.3874 -c copy -y ${file}/draft/PvVlagKo/media/clipVideo/gop_df5c3e6.ts")
        run("-i ${file}/draft/PvVlagKo/media/clipVideo/gop_df5c3e6.ts -ss 0.161633 -t 0.227589 -c:v libopenh264 -b:v 8000k -minrate 8000k -maxrate 8000k -bf 0 -c:a copy -y ${file}/draft/PvVlagKo/media/clipVideo/decode_-5eb5b92d.ts")
        run("-ss 338.034778 -i ${file}/draft/PvVlagKo/media/concatMp4/concat.mp4 -t 21.3032 -c copy -y ${file}/draft/PvVlagKo/media/clipVideo/copy_-dba0ac2.ts")
        val txtFile = File("${file}/draft/PvVlagKo/media/clipVideo/KLszZ.txt")
        val writer = FileWriter(txtFile)
        writer.write("file '${file}/draft/PvVlagKo/media/clipVideo/decode_-5eb5b92d.ts'\n")
        writer.write("file '${file}/draft/PvVlagKo/media/clipVideo/copy_-dba0ac2.ts'\n")
        writer.flush()
        run("-f concat -safe 0 -i ${file}/draft/PvVlagKo/media/clipVideo/KLszZ.txt -c copy ${file}/draft/PvVlagKo/media/clipVideo/-6853461.ts")
    }

    private suspend fun testAVCMaxMediaCodecCount() {
        val cap = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .filter { it.isEncoder }
            .filter { it.supportedTypes.contains("video/avc") }

        cap.forEach {
            val cap = it.getCapabilitiesForType("video/avc")

            log(
                """  
                CodecInfo:====>                
                    name: ${it.name}  
                    softwareOnly: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.isSoftwareOnly else "unknow"}  
                    maxSupportedInstances: ${cap.maxSupportedInstances}
                    """.trimIndent()
            )
        }
    }


}