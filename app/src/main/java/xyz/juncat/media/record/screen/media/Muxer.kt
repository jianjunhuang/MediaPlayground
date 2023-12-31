package xyz.juncat.media.record.screen.media

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

abstract class Muxer : Object() {

    var isStarted: Boolean = false

    abstract fun startRecording()

    abstract fun stopRecording()

    abstract fun pauseRecording()

    abstract fun resumeRecording()

    abstract fun addEncoder(encoder: MediaEncoder)

    abstract fun removeEncoder(encoder: MediaEncoder)

    abstract fun start(): Boolean

    abstract fun stop()

    abstract fun addTrack(format: MediaFormat): Int

    abstract fun writeSampleData(
        trackIndex: Int,
        byteBuf: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )

    abstract fun prepare()
}