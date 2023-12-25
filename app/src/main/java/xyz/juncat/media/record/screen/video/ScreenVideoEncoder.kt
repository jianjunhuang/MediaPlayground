package xyz.juncat.media.record.screen.video

import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import xyz.juncat.media.record.screen.config.VideoConfig
import xyz.juncat.media.record.screen.media.Muxer

class ScreenVideoEncoder(
    muxer: Muxer,
    private val config: VideoConfig,
    private val projection: MediaProjection
) : BaseVideoEncoder(
    muxer, config
) {

    override fun bindInputSurface() {
        projection.createVirtualDisplay(
            "test",
            config.width,
            config.height,
            2,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,//TODO 待扩展
            mediaCodec?.createInputSurface(),
            //surface
            null,
            null
        )
    }

    override fun stopRecording() {
        super.stopRecording()
        projection.stop()
    }
}