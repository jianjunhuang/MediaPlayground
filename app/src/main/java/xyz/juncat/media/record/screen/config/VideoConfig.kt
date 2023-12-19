package xyz.juncat.media.record.screen.config

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build

data class VideoConfig(
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val fps: Int,
    val iFrameInterval: Int,
    val fpsMode: FPSMode,
    val bitrateMode: BitrateMode
) {

    enum class FPSMode {
        VFR, CFR
    }

    enum class BitrateMode {
        CQ, VBR, CBR, CBR_FD
    }

}

val VideoConfig.BitrateMode.value: Int
    get() {
        return when (this) {
            VideoConfig.BitrateMode.CQ -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            VideoConfig.BitrateMode.VBR -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            VideoConfig.BitrateMode.CBR -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
            VideoConfig.BitrateMode.CBR_FD -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR_FD
            } else {
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            }
        }
    }

