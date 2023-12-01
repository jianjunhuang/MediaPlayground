package xyz.juncat.media.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer


/**
 * [sampleRateIndex]
 * 0: 96000 Hz
 * 1: 88200 Hz
 * 2: 64000 Hz
 * 3: 48000 Hz
 * 4: 44100 Hz
 * 5: 32000 Hz
 * 6: 24000 Hz
 * 7: 22050 Hz
 * 8: 16000 Hz
 * 9: 12000 Hz
 * 10: 11025 Hz
 * 11: 8000 Hz
 * 12: 7350 Hz
 * 13: Reserved
 * 14: Reserved
 * 15: frequency is written explictly
 *
 * [channel]
 * 0: Defined in AOT Specifc Config
 * 1: 1 channel: front-center
 * 2: 2 channels: front-left, front-right
 * 3: 3 channels: front-center, front-left, front-right
 * 4: 4 channels: front-center, front-left, front-right, back-center
 * 5: 5 channels: front-center, front-left, front-right, back-left, back-right
 * 6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
 * 7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
 * 8-15: Reserved
 *
 */
class AACOutputWrapper(
    private val outputPath: String, private val sampleRateIndex: Int, private val channel: Int
) {

    @get:Synchronized
    var isStarted: Boolean
        private set

    @get:Synchronized
    @Volatile
    var isPaused = false
        private set

    private var mFileOutputStream: FileOutputStream? = null
    private var mPCMOutputStream: FileOutputStream? = null
    private var mPCMSSRCOutputStream: FileOutputStream? = null

    init {
        try {
            val output = File(outputPath)
            if (output.exists()) {
                output.deleteRecursively()
            }
            output.mkdirs()
            val file =
                File(
                    output,
                    "ext.aac"
                )
            if (file.exists()) {
                file.delete()
            }
            mFileOutputStream = FileOutputStream(file)

            val pcmFile =
                File(
                    output,
                    "ext.pcm"
                )
            if (pcmFile.exists()) {
                pcmFile.delete()
            }
            mPCMOutputStream = FileOutputStream(pcmFile)

            val pcmSSRCFile =
                File(
                    output,
                    "ext_ssrc.pcm"
                )
            if (pcmSSRCFile.exists()) {
                pcmSSRCFile.delete()
            }
            mPCMSSRCOutputStream = FileOutputStream(pcmSSRCFile)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
        if (DEBUG) Log.i(TAG, "AAC Output: " + this.outputPath)
        isStarted = false
    }

    /**
     * write encoded data to muxer
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    /*package*/
    @Synchronized
    fun writeSampleData(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        try {
            byteBuf.position(bufferInfo.offset)
            byteBuf.limit(bufferInfo.offset + bufferInfo.size)

            if (bufferInfo.size - bufferInfo.offset <= 0) return
            val header =
                createAdtsHeader(bufferInfo.size - bufferInfo.offset, sampleRateIndex, channel)
            mFileOutputStream?.write(header)

            val data = ByteArray(byteBuf.remaining())
            byteBuf.get(data)
            mFileOutputStream?.write(data)
            mFileOutputStream?.flush()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * 添加ADTS头
     *
     */
    private fun createAdtsHeader(length: Int, sampleRateIndex: Int, channel: Int): ByteArray {
        val frameLength = length + 7
        val adtsHeader = ByteArray(7)
        adtsHeader[0] = 0xFF.toByte() // Sync Word
        adtsHeader[1] = 0xF1.toByte() // MPEG-4, Layer (0), No CRC
        adtsHeader[2] =
            ((MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1 shl 6) + (sampleRateIndex shl 2) + (channel shr 2)).toByte()
        adtsHeader[3] = (((channel and 3) shl 6) + (frameLength shr 11)).toByte()
        adtsHeader[4] = (frameLength and 0x7FF shr 3).toByte()
        adtsHeader[5] = (((frameLength and 0x07) shl 5) + 0x1F).toByte()
        adtsHeader[6] = 0xFC.toByte()
        return adtsHeader
    }

    //**********************************************************************
    //**********************************************************************
    companion object {
        private val DEBUG: Boolean
            get() = true
        private const val TAG = "MediaMuxerWrapper"
    }

    fun release() {
        mFileOutputStream?.close()
        mPCMSSRCOutputStream?.close()
        mPCMOutputStream?.close()
    }


    fun writePCM(buf: ByteBuffer) {
        mPCMOutputStream?.write(ByteArray(buf.remaining()).apply { buf.get(this) })
        mPCMOutputStream?.flush()
    }

    fun writeSSRCPCM(buf: ByteBuffer) {
        mPCMSSRCOutputStream?.write(ByteArray(buf.remaining()).apply { buf.get(this) })
        mPCMSSRCOutputStream?.flush()
    }
}

val Int.toSampleRateIndex: Int
    get() = when (this) {
        96000 -> 0
        88200 -> 1
        64000 -> 2
        48000 -> 3
        44100 -> 4
        32000 -> 5
        24000 -> 6
        22050 -> 7
        16000 -> 8
        12000 -> 9
        11025 -> 10
        8000 -> 11
        7350 -> 12
        else -> 4
    }