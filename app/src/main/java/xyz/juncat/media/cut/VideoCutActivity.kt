package xyz.juncat.media.cut

import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.google.android.material.button.MaterialButton
import com.jianjun.base.ext.setLayoutParamsSize
import com.jianjun.base.ext.setMatchParent
import com.jianjun.base.ext.setMatchWidth
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.TransformationListener
import com.linkedin.android.litr.analytics.TrackTransformationInfo
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.render.AudioRenderer
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.render.Renderer
import com.linkedin.android.litr.utils.MediaFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xyz.juncat.media.base.Utils
import java.io.File

class VideoCutActivity : AppCompatActivity() {

    private lateinit var textView: AppCompatTextView
    private val folderName = "cut"
    private lateinit var folderFile: File
    private lateinit var mediaTransformer: MediaTransformer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        folderFile = File(externalCacheDir, folderName)
        if (folderFile.exists()) folderFile.deleteRecursively()
        if (!folderFile.exists()) folderFile.mkdir()
        textView = AppCompatTextView(this).apply {
            setMatchParent()
        }
        val scrollView = NestedScrollView(this).apply {
            setLayoutParamsSize(ViewGroup.LayoutParams.MATCH_PARENT, 0)
            layoutParams = LinearLayout.LayoutParams(layoutParams).apply {
                weight = 1f
            }
        }
        val uri = intent.data ?: return
        val file = Utils.copyInternal(this, uri, folderName)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setMatchParent()
            val ffmpegRadio: RadioButton
            val litrRadio: RadioButton
            addView(RadioGroup(this@VideoCutActivity).apply {
                setMatchWidth()
                ffmpegRadio = RadioButton(this@VideoCutActivity).apply {
                    text = "FFmpeg"
                }
                addView(ffmpegRadio)
                litrRadio = RadioButton(this@VideoCutActivity).apply {
                    text = "Litr"
                }
                addView(litrRadio)
            })
            val edt = EditText(this@VideoCutActivity).apply {
                setMatchWidth()
                hint = "cut pos in millisecond. Default 1200"
                inputType = EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
            }
            addView(edt)
            addView(MaterialButton(this@VideoCutActivity).apply {
                setMatchWidth()
                text = "start cut"
                setOnClickListener {
                    val pos = edt.text.toString().toDoubleOrNull() ?: 1200.0
                    if (ffmpegRadio.isChecked) {
                        cutByFFmpeg(file, pos / 1000)
                    } else if (litrRadio.isChecked) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            cutByLitr(file, pos)
                        }
                    } else {
                        lifecycleScope.launchWhenCreated {
                            textView.appendL("set cut method first!")
                        }
                    }
                }
            })
            addView(scrollView.apply {
                addView(textView)
            })
        })
    }

    private fun cutByLitr(file: File, cutPos: Double) {
        val start = System.currentTimeMillis()
        mediaTransformer = MediaTransformer(this)
        val uri = file.toUri()
        val outputFile = File(folderFile, "litr.mp4")
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        parcelFileDescriptor?.fileDescriptor?.let {
            //extractor
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(it)

            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(it)
            val duration =
                (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong() ?: 0) * 1000
            mediaMetadataRetriever.release()

            val count = mediaExtractor.trackCount

            val mediaTarget = MediaMuxerMediaTarget(
                outputFile.path,
                count,
                0,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val range = MediaRange((cutPos * 1000000).toLong(), duration)
            lifecycleScope.launch {
                textView.appendL("range: ${(range.end - range.start) / 1000000}s")
            }
            val mediaSource =
                MediaExtractorMediaSource(
                    this,
                    uri,
                    range
                )
            val trackTransform = mutableListOf<TrackTransform>()
            for (targetTrack in 0 until count) {
                val encoder = MediaCodecEncoder()
                val format = mediaExtractor.getTrackFormat(targetTrack)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue
                var render: Renderer? = null
                val mediaFormat = if (mimeType.startsWith("video")) {
                    render = GlVideoRenderer(listOf())
                    MediaFormat.createVideoFormat(
                        "video/avc",
                        format.getInteger(MediaFormat.KEY_WIDTH),
                        format.getInteger(MediaFormat.KEY_HEIGHT)
                    ).apply {
                        setInteger(
                            MediaFormat.KEY_FRAME_RATE,
                            MediaFormatUtils.getFrameRate(format, 24).toInt()
                        )
                        setInteger(
                            MediaFormat.KEY_I_FRAME_INTERVAL,
                            MediaFormatUtils.getIFrameInterval(format, -1).toInt()
                        )
                        setInteger(
                            MediaFormat.KEY_BIT_RATE,
                            getInt(format, MediaFormat.KEY_BIT_RATE, 5000000)
                        )
                        setLong(
                            MediaFormat.KEY_DURATION,
                            format.getLong(MediaFormat.KEY_DURATION)
                        )
                        setInteger(MediaFormat.KEY_ROTATION, 0)
                    }
                } else if (mimeType.startsWith("audio")) {
                    render = AudioRenderer(encoder, null)
                    MediaFormat.createAudioFormat(
                        MediaFormat.MIMETYPE_AUDIO_AAC,
                        MediaFormatUtils.getSampleRate(format, -1).toInt(),
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    ).apply {
                        setInteger(
                            MediaFormat.KEY_BIT_RATE,
                            getInt(format, MediaFormat.KEY_BIT_RATE, 128000)
                        )
                        setLong(
                            MediaFormat.KEY_DURATION,
                            format.getLong(MediaFormat.KEY_DURATION)
                        )
                    }
                } else {
                    null
                }

                val builder = TrackTransform.Builder(mediaSource, targetTrack, mediaTarget)
                    .setTargetTrack(trackTransform.size)
                    .setTargetFormat(mediaFormat)
                    .setEncoder(encoder)
                    .setDecoder(MediaCodecDecoder())
                if (render != null) {
                    builder.setRenderer(render)
                }
                trackTransform.add(
                    builder.build()
                )

            }


            mediaTransformer.transform(
                "test",
                trackTransform,
                object : TransformationListener {
                    override fun onStarted(id: String) {
                        lifecycleScope.launch {
                            textView.appendL("onStart")
                        }
                    }

                    override fun onProgress(id: String, progress: Float) {
                        lifecycleScope.launch {
                            textView.appendL("onProgress: $progress")
                        }
                    }

                    override fun onCompleted(
                        id: String,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        lifecycleScope.launch {
                            textView.appendL("onCompleted: duration=${System.currentTimeMillis() - start}")
                        }
                    }

                    override fun onCancelled(
                        id: String,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        lifecycleScope.launch {
                            textView.appendL("onCancelled")
                        }
                    }

                    override fun onError(
                        id: String,
                        cause: Throwable?,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        lifecycleScope.launch {
                            textView.appendL("onError:" + cause?.toString())
                        }
                    }

                },
                MediaTransformer.GRANULARITY_DEFAULT
            );
        }
    }

    private fun getInt(format: MediaFormat, key: String, defaultValue: Int): Int {
        if (format.containsKey(key)) {
            return format.getInteger(key)
        }
        return defaultValue
    }

    private fun cutByFFmpeg(file: File, cutPos: Double) {
        val start = System.currentTimeMillis()
        val ptsFile = File(file.parent, "pts.xml")
        val getPtsCmd =
            "-loglevel error -select_streams v:0 -show_entries packet=pts_time,flags -of json -i ${file.path}"
        lifecycleScope.launch(Dispatchers.IO) {

            textView.appendL("\n--> get keyframe's pts")
            val session = FFprobeKit.execute(getPtsCmd)
            textView.appendL("${(session.endTime.time - session.startTime.time)}ms")
            val array = JSONObject(session.output)
                .getJSONArray("packets")
            val ptsArr = ArrayList<String>()
            for (index in 0 until array.length()) {
                val data = array.getJSONObject(index)
                if ("K_" == data.getString("flags")) {
                    val pts = data.getString("pts_time")
                    textView.appendL(pts)
                    ptsArr.add(pts)
                }
            }
            val ptsDouble = ptsArr.map {
                it.toDouble()
            }
            var k1 = 0.0
            var k2 = 0.0
            //find k1, k2
            for (index in ptsDouble.indices) {
                val p = ptsDouble[index]
                if (p > cutPos) {
                    k1 = if (index == 0) {
                        0.0
                    } else {
                        ptsDouble[index - 1]
                    }
                    k2 = p
                    break
                }
            }
            if (k1 == 0.0 && k2 == 0.0) {
                textView.appendL("cut pos($cutPos) is not in keyframes. k2=$k2, k1=$k1", true)
                return@launch
            }
            //encode gop to all I frame
            textView.appendL("\n--> Encode gop to all I Frame")
            val gopFile = File(folderFile, "gop.mp4")
            val convertGOPCmd = "-ss $k1 -i ${file.path} -t ${k2 - k1} -c copy ${gopFile}"
            textView.appendL(convertGOPCmd)
            val gopSession = FFmpegKit.execute(convertGOPCmd)
            if (gopSession.returnCode.isValueSuccess) {
                textView.appendL(gopFile.absolutePath)
            } else {
                textView.appendL(gopSession.logsAsString, true)
            }

            //cut start
            val startFile = File(folderFile, "start.mp4")
            val cutStartCmd = "-ss 0 -i ${file.path} -t $k1 -c copy ${startFile.path}"

            //cut end
            textView.appendL("\n--> Cut to end")
            val endFile = File(folderFile, "end.ts")
            val cutEndCmd = "-ss $k2 -i ${file.path} -c copy ${endFile.path}"
            textView.appendL(cutEndCmd)
            val cutEndSession = FFmpegKit.execute(cutEndCmd)
            if (cutEndSession.returnCode.isValueSuccess) {
                textView.appendL(endFile.path)
            } else {
                textView.appendL("ERROR: " + cutEndSession.returnCode, true)
                textView.appendL(cutEndSession.logsAsString, true)
                return@launch
            }

            //cut gop
            textView.appendL("\n--> Cut GOP")
            val endGOPFile = File(folderFile, "gop_end.ts")
            val cutEndGOPCmd =
                "-i ${gopFile.path} -ss ${cutPos - k1} -c:v libopenh264 -profile:v high -c:a copy ${endGOPFile.path}"
            textView.appendL(cutEndGOPCmd)
            val cutEndGOPSession = FFmpegKit.execute(cutEndGOPCmd)
            if (cutEndGOPSession.returnCode.isValueSuccess) {
                textView.appendL("<--" + endGOPFile.path)
            } else {
                textView.appendL("ERROR: " + cutEndGOPSession.returnCode, true)
                textView.appendL(cutEndGOPSession.logsAsString, true)
                return@launch
            }

            //concat
            textView.appendL("\n--> concat GOP end and Video End")
//            val concatFile = File(folderFile, "concat.txt")
//            val fos = FileWriter(concatFile)
//            fos.write("file \'${endGOPFile.path}\'")
//            fos.write("\n")
//            fos.write("file \'${endFile.path}\'")
//            fos.flush()
//            fos.close()
            val concatMp4File = File(folderFile, "concat.mp4")
            //https://trac.ffmpeg.org/wiki/Concatenate
//            val concatCmd = "-f concat -safe 0 -i ${concatFile.path} ${concatMp4File.path}"
            val concatCmd =
                "-i \"concat:${endGOPFile.path}|${endFile.path}\" -c copy ${concatMp4File.path}"
            textView.appendL(concatCmd)
            val concatSession = FFmpegKit.execute(concatCmd)
            if (concatSession.returnCode.isValueSuccess) {
                textView.appendL("<--" + concatMp4File.path)
            } else {
                textView.appendL(concatSession.allLogsAsString, true)
                return@launch
            }

            textView.appendL("\ncut duration: ${System.currentTimeMillis() - start}")

            val resultDurationSession =
                FFprobeKit.execute("-i ${concatMp4File.path} -show_entries format=duration")
            textView.appendL("result duration: ")
            textView.appendL(resultDurationSession.allLogsAsString)
            //cut dirt
//            textView.appendL("\n--> cut dirt")
//            val cutDirtFile = File(folderFile, "cut_dirt.mp4")
//            val cutDirtCmd = "-ss $cutPos -i ${file.path} ${cutDirtFile.path}"
//            val cutDirtSession = FFmpegKit.execute(cutDirtCmd)
//            if (cutDirtSession.returnCode.isValueSuccess) {
//                textView.appendL("<--" + cutDirtFile.path)
//            } else {
//                textView.appendL(cutDirtSession.logsAsString)
//            }
        }
    }

    private suspend fun AppCompatTextView.appendL(text: String, error: Boolean = false) {
        withContext(Dispatchers.Main) {
            if (!error) {
                append(text + "\n")
            } else {
                append(SpannableStringBuilder(text).apply {
                    setSpan(
                        ForegroundColorSpan(Color.RED),
                        0,
                        text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                })
            }
        }
    }
}