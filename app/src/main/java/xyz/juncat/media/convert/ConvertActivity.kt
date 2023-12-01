package xyz.juncat.media.convert

import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.StreamInformation
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.TransformationListener
import com.linkedin.android.litr.analytics.TrackTransformationInfo
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.render.AudioRenderer
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.render.Renderer
import com.linkedin.android.litr.utils.MediaFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ypresto.androidtranscoder.format.MediaFormatExtraConstants
import net.ypresto.androidtranscoder.format.MediaFormatStrategy
import xyz.juncat.media.R
import xyz.juncat.media.base.Utils
import java.io.File


class ConvertActivity : AppCompatActivity() {

    private lateinit var mediaTransformer: MediaTransformer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert)
        mediaTransformer = MediaTransformer(applicationContext)

        val uri = intent.data ?: return
//            VideoConvertor().convert(this@ConvertActivity, uri)

        lifecycleScope.launchWhenResumed {
            val file = withContext(Dispatchers.IO) {
                Utils.copyInternal(this@ConvertActivity, uri)
            }
            val outputFile = File(externalCacheDir, "convert_out.mp4")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            withContext(Dispatchers.IO) {
                convertByFFmpeg(file, outputFile)
            }
//            convertByTitr(file, uri)
//                MediaTranscoder.getInstance()
//                    .transcodeVideo(
//                        it,
//                        outputFile.absolutePath,
//                        MediaFormatStrategyPresets.createAndroid720pStrategy(videoBitrate.toInt(), audioBitrate.toInt(), 1),
//                        object : MediaTranscoder.Listener {
//                            override fun onTranscodeProgress(progress: Double) {
//                                Log.i(TAG, "onTranscodeProgress: $progress")
//                            }
//
//                            override fun onTranscodeCompleted() {
//                                Log.i(TAG, "onTranscodeCompleted: COMPLETED")
//                            }
//
//                            override fun onTranscodeCanceled() {
//                                Log.i(TAG, "onTranscodeCanceled: CANCELED")
//                            }
//
//                            override fun onTranscodeFailed(exception: Exception?) {
//                                Log.i(TAG, "onTranscodeFailed: $exception")
//                            }
//
//                        })
        }
    }

    private fun convertByFFmpeg(file: File, outputFile: File) {
        val cmd = "-i ${file.path} -c:v h264 -c:a aac ${outputFile.path}"
        val session = FFmpegKit.execute(cmd)
        Log.i(TAG, "convertByFFmpeg: ${session.returnCode.isValueSuccess}")
    }

    private fun convertByTitr(file: File, uri: Uri, outputFile: File) {
        var videoBitrate = 0L
        var videoFrameRate = 0L
        var audioChannel = 0L
        var audioBitrate = 0L
        var videoWidth = 0
        var videoHeight = 0
        var audioSample = 0
        var videoDuration = 0
        var audioDuration = 0
        for (stream in FFprobeKit.getMediaInformation(file.absolutePath).mediaInformation.streams) {
            if (stream.type == "video") {
                videoBitrate = stream.bitrate.toLong()
                videoFrameRate = 24
                videoWidth = stream.width.toInt()
                videoHeight = stream.height.toInt()
            } else if (stream.type == "audio") {
                audioChannel = 1
                audioBitrate = stream.getNumberProperty(StreamInformation.KEY_BIT_RATE)
                audioSample = stream.sampleRate.toInt()
            }
        }

        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        parcelFileDescriptor?.fileDescriptor?.let {


            //extractor
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(it)
            val count = mediaExtractor.trackCount

            val mediaTarget = MediaMuxerMediaTarget(
                outputFile.path,
                count,
                0,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val mediaSource = MediaExtractorMediaSource(this@ConvertActivity, uri)
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
                        setInteger(
                            MediaFormat.KEY_PROFILE,
                            MediaCodecInfo.CodecProfileLevel.AVCProfileMain
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            setInteger(
                                MediaFormat.KEY_LEVEL,
                                MediaCodecInfo.CodecProfileLevel.AVCLevel31
                            )
                        }
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
                        Log.i(TAG, "onStarted: ")
                    }

                    override fun onProgress(id: String, progress: Float) {

                    }

                    override fun onCompleted(
                        id: String,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        Log.i(TAG, "onCompleted: ")
                    }

                    override fun onCancelled(
                        id: String,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        Log.i(TAG, "onCancelled: ")
                    }

                    override fun onError(
                        id: String,
                        cause: Throwable?,
                        trackTransformationInfos: MutableList<TrackTransformationInfo>?
                    ) {
                        Log.i(TAG, "onError: $cause")
                    }

                },
                MediaTransformer.GRANULARITY_DEFAULT
            );
        }
    }

    class H264Strategy(
        private val videoBitrate: Int,
        private val videoFrame: Int,
        private val audioChannel: Int,
        private val audioBitrate: Int
    ) : MediaFormatStrategy {
        override fun createVideoOutputFormat(inputFormat: MediaFormat?): MediaFormat {
            val width = inputFormat!!.getInteger(MediaFormat.KEY_WIDTH)
            val height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val format = MediaFormat.createVideoFormat("video/avc", width, height)
            format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrame)
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            return format
        }

        override fun createAudioOutputFormat(inputFormat: MediaFormat?): MediaFormat {
            val format = MediaFormat.createAudioFormat(
                MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                inputFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE), audioChannel
            )
            format.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
            return format
        }

    }

    private fun getInt(format: MediaFormat, key: String, defaultValue: Int): Int {
        if (format.containsKey(key)) {
            return format.getInteger(key)
        }
        return defaultValue
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    companion object {
        private const val TAG = "ConvertActivity"
    }
}