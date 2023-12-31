package xyz.juncat.media.record.screen.video

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import xyz.juncat.media.record.screen.config.VideoConfig
import xyz.juncat.media.record.screen.config.value
import xyz.juncat.media.record.screen.media.MediaEncoder
import xyz.juncat.media.record.screen.media.Muxer

abstract class BaseVideoEncoder(
    muxer: Muxer,
    private val config: VideoConfig,
) : MediaEncoder(muxer) {

    override fun prepare() {
        super.prepare()
        mediaCodec = MediaCodec.createEncoderByType("video/avc")
        val mediaFormat =
            MediaFormat.createVideoFormat("video/avc", config.width, config.height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(
                    MediaFormat.KEY_BIT_RATE, config.bitrate
                )
                setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval
                )
                setInteger(
                    MediaFormat.KEY_BITRATE_MODE, config.bitrateMode.value
                )
                //MUST. WHY?
                setInteger(
                    MediaFormat.KEY_FRAME_RATE, config.fps
                )
            }
        mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        bindInputSurface()
        mediaCodec?.start()
    }

    abstract fun bindInputSurface()

    override fun signalEndOfInputStream() {
        mediaCodec?.signalEndOfInputStream()
        isEOS = true
    }

}