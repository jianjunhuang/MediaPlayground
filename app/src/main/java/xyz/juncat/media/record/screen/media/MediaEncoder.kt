package xyz.juncat.media.record.screen.media

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import java.lang.ref.WeakReference
import kotlin.concurrent.Volatile

abstract class MediaEncoder(muxer: MediaMuxer) : Runnable {

    protected lateinit var bufferInfo: BufferInfo

    protected val mediaMuxer = WeakReference<MediaMuxer>(muxer)

    @Volatile
    protected var isRequestPause = false

    @Volatile
    protected var isCapturing = false

    @Volatile
    protected var isRequestStop = false

    protected var mediaCodec: MediaCodec? = null

    private val TIMEOUT = 10000L

    protected var isEOS = false

    init {
        muxer.addEncoder(this)
        bufferInfo = BufferInfo()
        Thread(this, this.javaClass.simpleName).start()
    }

    override fun run() {

        while (true) {
            if (isRequestStop) {
                //drain remain
                drain()
                //EOS TODO
                drain()
                release()
                break
            }
            drain()
        }
        isRequestStop = true
        isCapturing = false
        //TODO onStop
    }

    protected fun drain() {
        if (mediaCodec == null) return
        var retryCount = 0
        while (isCapturing) {
            val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, TIMEOUT)
                ?: MediaCodec.INFO_TRY_AGAIN_LATER
            when (encoderStatus) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!isEOS && ++retryCount > 5) {
                        break
                    }
                }

                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    //todo add track
                    mediaCodec?.outputFormat?.let {
                        val track = mediaMuxer.get()?.addTrack(it)
                    } ?: throw IllegalStateException("MeidaCodec is NULL")

                    if (mediaMuxer.get()?.start() == false) {
                        synchronized(mediaMuxer) {
                            //todo wait muxer start
                        }
                    }

                }

                else -> {
                    if (encoderStatus < 0) {
                        //ignore
                        continue
                    }
                    //todo
                }
            }
        }
    }

    protected fun prepare() {

    }

    protected fun startRecording() {

    }

    protected fun stopRecording() {

    }

    protected fun pauseRecording() {

    }

    protected fun resumeRecording() {

    }

    protected fun release() {

    }
}