package xyz.juncat.media.record.screen.media

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.concurrent.Volatile

abstract class MediaEncoder(muxer: MediaMuxer) : Runnable {

    protected var bufferInfo: BufferInfo

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

    protected var trackIndex = -1

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
                //EOS
                signalEndOfInputStream()
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

                    val muxer = mediaMuxer.get() ?: throw IllegalArgumentException("muxer is NULL")
                    mediaCodec?.outputFormat?.let {
                        trackIndex = muxer.addTrack(it)
                    } ?: throw IllegalStateException("MediaCodec is NULL")

                    if (!muxer.start()) {
                        synchronized(muxer) {
                            while (!muxer.isStarted) {
                                try {
                                    muxer.wait(100)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                    //todo should stop capturing?
                                    break
                                }
                            }
                        }
                    }

                }

                else -> {
                    if (encoderStatus < 0) {
                        //ignore TODO WARNING
                        continue
                    }
                    val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                        ?: throw IllegalStateException("encoderOutputBuffer was NULL")
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        retryCount = 0
                        if (!onBufferEncoded(trackIndex, encodedData, bufferInfo)) {
                            mediaMuxer.get()?.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    //release
                    mediaCodec?.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        //EOS
                        isCapturing = false
                        break
                    }
                }
            }
        }
    }

    private fun signalEndOfInputStream() {
        doEncode(null, -1 ,0)
    }

    private fun doEncode(buffer: ByteBuffer?, length: Int, ptsUs: Long): Boolean {
        val inputBufferIndex = mediaCodec?.dequeueInputBuffer(TIMEOUT) ?: -1
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            if (buffer != null) {
                inputBuffer?.put(buffer)
            }
            if (length < 0) {
                isEOS = true
                mediaCodec?.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    ptsUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            } else {
                mediaCodec?.queueInputBuffer(inputBufferIndex, 0, length, ptsUs, 0)
            }
            return true
        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //ignore wait MediaCodec ready
        }
        return true
    }

    protected fun onBufferEncoded(
        trackIndex: Int,
        encodedData: ByteBuffer,
        bufferInfo: BufferInfo
    ): Boolean = false

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

    companion object {
        private const val TAG = "MediaEncoder"
    }
}