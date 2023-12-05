package xyz.juncat.media.record.screen.config

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