package xyz.juncat.media.convert

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File

class VideoConvertor {

    private var muxer: MediaMuxer? = null

    private var videoDecoder: MediaCodec? = null
    private var audioDecoder: MediaCodec? = null

    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null

    private var videoExtractor: MediaExtractor? = null
    private var audioExtractor: MediaExtractor? = null

    fun convert(context: Context, videoUri: Uri) {
        //muxer
        val outputFile = File(context.externalCacheDir, "convert_out.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        videoExtractor = MediaExtractor().apply {
            //parames
            var videoFormatType: String = ""
            var videoTrackIndex = 0
            var width: Int = 0
            var height: Int = 0
            var frameRate = 0
            var bitRate = 0
            setDataSource(context, videoUri, null)
            for (index in 0 until trackCount) {
                val format = getTrackFormat(index)
                val formatType = format.getString(MediaFormat.KEY_MIME)
                if (formatType?.startsWith("video") == true) {
                    videoTrackIndex = index
                    videoFormatType = formatType
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    selectTrack(index)
                    break
                }
            }

            videoDecoder = MediaCodec.createDecoderByType(videoFormatType).apply {
                configure(getTrackFormat(videoTrackIndex), null, null, 0)
                start()
            }

            val encodeFormat =
                MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            encodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            encodeFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            val name = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(encodeFormat)
            videoEncoder = MediaCodec.createByCodecName(name)
            videoEncoder?.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoEncoder?.start()

        }
    }

//    private fun selectCodec(mimeType: String): MediaCodecInfo {
//        MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat()
//
//    }

    fun start() {
        inputThread.start()
        outputThread.start()
    }

    private val TIME_US = 70000L

    private val inputThread = Thread {
        val info = MediaCodec.BufferInfo()
        var readEOS = false
        while (true) {
            if (!readEOS) {
                val inputIndex = videoDecoder?.dequeueInputBuffer(TIME_US) ?: -1
                if (inputIndex >= 0) {
                    val inputBuffer = videoDecoder?.getInputBuffer(inputIndex) ?: return@Thread
                    val size = videoExtractor?.readSampleData(inputBuffer, 0) ?: 0
                    if (size > 0) {
                        videoDecoder?.queueInputBuffer(
                            inputIndex,
                            0,
                            size,
                            videoExtractor?.sampleTime ?: 0,
                            videoExtractor?.sampleFlags ?: 0
                        )
                        videoExtractor?.advance()
                    } else {
                        videoDecoder?.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        readEOS = true
                    }
                }
            }
            val outputIndex = videoDecoder?.dequeueOutputBuffer(info, TIME_US) ?: -1
            if (outputIndex >= 0) {
                val outputBuffer = videoDecoder?.getOutputBuffer(outputIndex)
                val inputBufferEncodeIndex = videoEncoder?.dequeueInputBuffer(TIME_US) ?: -1
                if (inputBufferEncodeIndex >= 0) {
                    val inputEncodeBuffer = videoEncoder?.getInputBuffer(inputBufferEncodeIndex)
                    if (info.size >= 0) {
                        inputEncodeBuffer?.put(outputBuffer)
                        videoEncoder?.queueInputBuffer(
                            inputBufferEncodeIndex,
                            0,
                            info.size,
                            info.presentationTimeUs,
                            info.flags
                        )
                    } else {
                        videoEncoder?.queueInputBuffer(
                            inputBufferEncodeIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    }
                }
            }
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break
            }
        }
        videoExtractor?.release()
        videoDecoder?.stop()
        videoDecoder?.release()
    }

    private var videoTrackIndex = -1
    private var isMuxerStarted = false
    private var videoTime = 0L

    private val outputThread = Thread {
        val mediaInfo = MediaCodec.BufferInfo()
        var info: MediaCodec.BufferInfo? = null
        while (true) {
            val outputBufferIndex = videoEncoder?.dequeueOutputBuffer(mediaInfo, TIME_US) ?: -1
            when (outputBufferIndex) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> break
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = videoEncoder?.getOutputFormat() ?: break
                    if (videoTrackIndex < 0) {
                        videoTrackIndex = muxer?.addTrack(format) ?: -1
                    }
                    if (!isMuxerStarted) {
                        muxer?.start()
                        isMuxerStarted = true
                    }
                }

                else -> {
                    val outputBuffer = videoEncoder?.getOutputBuffer(outputBufferIndex) ?: continue
                    if (mediaInfo.size >= 0 && isMuxerStarted) {
                        info = MediaCodec.BufferInfo()
                        info.size = mediaInfo.size
                        info.offset = mediaInfo.offset
                        info.presentationTimeUs = videoTime + mediaInfo.presentationTimeUs
                        info.flags = mediaInfo.flags
                        muxer?.writeSampleData(videoTrackIndex, outputBuffer, info)
                    }

                    videoEncoder?.releaseOutputBuffer(outputBufferIndex, true)
                }
            }
            if ((mediaInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                videoTime = info?.presentationTimeUs ?: 0L
                break
            }
        }

        videoEncoder?.start()
        videoEncoder?.release()

        muxer?.stop()
        muxer?.release()
    }

}