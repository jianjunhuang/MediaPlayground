package xyz.juncat.media.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface
import xyz.juncat.media.record.RecordActivity.Companion.TAG

class MediaCodecRecorder : RecordActivity.IRecorder {
        private var muxerStarted = false
        private var muxer: MediaMuxer? = null

        private var audioTrackIndex = -1
        private var micMediaCodec: MediaCodec? = null
        private var audioMediaCodec: MediaCodec? = null

        companion object {
            private val AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
            private val AUDIO_SAMPLE_RATE = 44100
            private val AUDIO_BIT_RATE = 64000
            const val AUDIO_SAMPLES_PER_FRAME = 1024 // AAC, bytes/frame/channel

            const val AUDIO_FRAMES_PER_BUFFER = 25 // AAC, frame/buffer/sec

        }

        private var videoTrackIndex = -1
        private var videoMediaCodec: MediaCodec? = null
        private val VIDEO_MIME_TYPE = "video/avc"
        private var videoPath: String = ""

        override fun init(mediaProjection: MediaProjection?) {
            videoMediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
        }

        override fun prepare(width: Int, height: Int, videoPath: String): Surface {
            this.videoPath = videoPath
            val frameRate = 30
            val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 6000000)//6Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate)
                setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate)
                setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)// 1 seconds between I-frames
            }
            videoMediaCodec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoMediaCodec?.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    Log.i(TAG, "Input buffer avail")
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
                ) {
                    videoMediaCodec?.getOutputBuffer(index)?.let {
                        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            info.size = 0
                        }
                        if (info.size != 0) {
                            if (muxerStarted) {
                                it.position(info.offset)
                                it.limit(info.offset + info.size)
                                muxer?.writeSampleData(videoTrackIndex, it, info)
                            }
                        }
                        videoMediaCodec?.releaseOutputBuffer(index, false)
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    Log.i(TAG, "MediaCodec ${codec.name}, onError: $e")
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.i(TAG, "Output Format changed")
                    if (videoTrackIndex >= 0) {
                        throw RuntimeException("format changed twice")
                    }
                    videoMediaCodec?.outputFormat?.let {
                        videoTrackIndex = muxer?.addTrack(it) ?: -1
                    }
                    if (!muxerStarted && videoTrackIndex >= 0) {
                        muxer?.start()
                        muxerStarted = true
                    }
                }

            })
            return videoMediaCodec?.createInputSurface()
                ?: throw RuntimeException("failed to create surface")
        }

        override fun start() {
            videoMediaCodec?.start()
            muxer = MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        override fun destroy() {
            if (muxerStarted) {
                muxer?.stop()
            }
            muxer?.release()
            videoMediaCodec?.run {
                stop()
                release()
            }
            videoTrackIndex = -1

            audioMediaCodec?.run {
                stop()
                release()
                audioTrackIndex = -1
            }
        }
    }
