package xyz.juncat.media.record.screen.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

class MP4Muxer(path: String) : Muxer() {

    @Volatile
    private var trackCount = 0

    private val encoders = ArrayList<MediaEncoder>()

    private val muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    override fun startRecording() {
        encoders.forEach {
            it.startRecording()
        }
    }

    override fun stopRecording() {
        encoders.forEach {
            it.stopRecording()
        }
    }

    override fun pauseRecording() {
        encoders.forEach {
            it.pauseRecording()
        }
    }

    override fun resumeRecording() {
        encoders.forEach {
            it.resumeRecording()
        }
    }

    override fun addEncoder(encoder: MediaEncoder) {
        encoders.add(encoder)
    }

    override fun start(): Boolean {
        if (trackCount == encoders.size) {
            Log.i(TAG, "start: ")
            muxer.start()
            return true
        }
        return false
    }

    override fun stop() {
        muxer.stop()
    }

    override fun addTrack(format: MediaFormat): Int {
        trackCount++
        return muxer.addTrack(format)
    }

    override fun writeSampleData(
        trackIndex: Int,
        byteBuf: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        muxer.writeSampleData(trackIndex, byteBuf, bufferInfo)
    }

    override fun release() {
        //TODO wait all encoder release
        muxer.stop()
        muxer.release()
    }

    override fun prepare() {
        encoders.forEach {
            it.prepare()
        }
    }

    companion object {
        private const val TAG = "MP4Muxer"
    }
}