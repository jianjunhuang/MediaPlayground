package xyz.juncat.media.record

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Process
import android.util.Log
import io.github.nailik.androidresampler.Resampler
import io.github.nailik.androidresampler.ResamplerConfiguration
import io.github.nailik.androidresampler.data.ResamplerChannel
import io.github.nailik.androidresampler.data.ResamplerQuality
import xyz.juncat.media.Convertor
import java.nio.ByteBuffer

class MyAudioEncoder(
    private val outputPath: String,
    private val recordSampleRate: Int,
    private val recordChannel: Int,
    private val encodeSampleRate: Int,
    private val encodeChannel: Int,
) {

    private var isRequestStop = false
    private var audioEncoder: MediaCodec? = null
    private val MIME_TYPE = "audio/mp4a-latm"
    private var isEOS = false
    private val bufferInfo = BufferInfo()
    private var startPTSUs = 0L
    private var audioRecord: AudioRecord? = null
    private val SAMPLES_PER_FRAME = 1024 // AAC, bytes/frame/channel
    private var isPause = false
    private var lastPauseTimeUs = 0L
    private var offsetPTSUs: Long = 0L
    private var prevOutputPTSUs: Long = 0

    private val convertor = Convertor()
    private val ssrcOutBuf = ByteBuffer.allocateDirect(1024)
    private var resampler: Resampler? = null

    private val outputWrapper by lazy {
        AACOutputWrapper(
            outputPath,
            encodeSampleRate.toSampleRateIndex,
            encodeChannel,
        )
    }

    @Volatile
    private var isCapturing = false

    private var audioThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun prepare() {
        val audioCodecInfo = selectAudioCodec(MIME_TYPE)
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for $MIME_TYPE")
            return
        }
        val audioFormat = createMediaFormat()
        audioEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
        audioEncoder?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioRecord = createAudioRecord()

//        convertor.init(
//            if (recordChannel == 1) Codec.ChannelLayout.MONO else Codec.ChannelLayout.STEREO,
//            Codec.SampleFormat.AV_SAMPLE_FMT_S16,
//            recordSampleRate,
//            if (encodeChannel == 1) Codec.ChannelLayout.MONO else Codec.ChannelLayout.STEREO,
//            Codec.SampleFormat.AV_SAMPLE_FMT_S16,
//            encodeSampleRate
//        )
        val configuration = ResamplerConfiguration(
            quality = ResamplerQuality.BEST,
            inputChannel = recordChannel.channel,
            inputSampleRate = recordSampleRate,
            outputChannel = encodeChannel.channel,
            outputSampleRate = encodeSampleRate
        )
        resampler = Resampler(configuration)
        audioEncoder?.start()
    }

    private val Int.channel: ResamplerChannel
        get() = when (this) {
            1 -> ResamplerChannel.MONO
            2 -> ResamplerChannel.STEREO
            else -> ResamplerChannel.STEREO
        }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(recordSampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(if (recordChannel == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO)
                            .build()
                    )
                    .build()
            } else {
                null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun createMediaFormat(): MediaFormat {
        val audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, encodeSampleRate, encodeChannel)
        audioFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }
        return audioFormat
    }

    fun start() {
        startPTSUs = System.nanoTime() / 1000
        isCapturing = true
        isPause = false
        isRequestStop = false
        if (audioThread == null) {
            audioThread = audioRecord?.let { AudioThread(it) }
            audioThread?.start()
            Log.d(TAG, "AudioThread start: ${audioThread != null}")
        }
    }

    fun stop() {
        if (!isCapturing) {
            return
        }
        isRequestStop = true
    }

    fun pause() {
        isPause = true
        lastPauseTimeUs = System.nanoTime() / 1000L
    }

    fun resume() {
        isPause = false
        if (lastPauseTimeUs > 0) {
            offsetPTSUs += ((System.nanoTime() / 1000L) - lastPauseTimeUs)
            lastPauseTimeUs = 0
        }
    }

    fun release() {
        isCapturing = false
        audioEncoder?.stop()
        audioEncoder?.release()
        audioEncoder = null
        outputWrapper.release()
//        convertor.close()
        audioRecord = null
    }

    fun encode(buffer: ByteBuffer, length: Int, presentationTimeUs: Long) {
        try {
            if (!isPause && doEncode(buffer, length, presentationTimeUs)) {
                handleOutput()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "encode: ", t)
        }

    }

    private fun doEncode(buffer: ByteBuffer, length: Int, presentationTimeUs: Long): Boolean {
        val inputBufferIndex = audioEncoder?.dequeueInputBuffer(0) ?: -1
        if (inputBufferIndex >= 0) {
            Log.i(TAG, "doEncode: state in $inputBufferIndex")
            val inputBuffer = audioEncoder?.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(buffer)
            if (length <= 0) {
                isEOS = true
                audioEncoder?.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            } else {
                audioEncoder?.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    length,
                    presentationTimeUs,
                    0
                )
            }
            return true
        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // no input buffer right now
            Log.i(TAG, "doEncode: state in INFO_TRY_AGAIN_LATER")
        }
        return false
    }

    private fun selectAudioCodec(mimeType: String): MediaCodecInfo? {
        var result: MediaCodecInfo? = null
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    result = codecInfo
                    return result
                }
            }
        }
        return result
    }


    companion object {
        private const val TAG = "ExtAudioEncoder"
    }

    private fun handleOutput() {
        var encoderOutputBuffers = audioEncoder?.outputBuffers
        var encoderStatus = audioEncoder?.dequeueOutputBuffer(
            bufferInfo,
            0
        ) ?: -1
        while (isCapturing && encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers =
                    audioEncoder?.outputBuffers
                Log.i(TAG, "handleOutput: state in INFO_TRY_AGAIN_LATER")
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                val newFormat = audioEncoder?.outputFormat
                Log.i(TAG, "handleOutput: state in INFO_OUTPUT_FORMAT_CHANGED")
                Log.d(TAG, "encoder output format changed: $newFormat")
            } else if (encoderStatus < 0) {
                Log.i(TAG, "handleOutput: state in $encoderStatus")
                Log.w(
                    TAG,
                    "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus"
                )
                // let's ignore it
            } else {
                Log.i(TAG, "handleOutput: state in $encoderStatus")
                val encodedData = encoderOutputBuffers?.get(encoderStatus)
                if (encodedData == null) {
                    Log.w(TAG, "encoderOutputBuffer $encoderStatus was null")
                } else {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        bufferInfo.presentationTimeUs = getPTS()
                        outputWrapper.writeSampleData(encodedData, bufferInfo)
                        prevOutputPTSUs = bufferInfo.presentationTimeUs
                        Log.d(
                            TAG,
                            "sent " + bufferInfo.size + " bytes to muxer, ts=" +
                                    bufferInfo.presentationTimeUs
                        )
                    }
                    encodedData.clear()
                    audioEncoder?.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        // when EOS come.
                        isCapturing = false
                        break // out of while
                    }
                }
            }
            encoderStatus = audioEncoder?.dequeueOutputBuffer(
                bufferInfo,
                0
            ) ?: -1
        }
    }

    private fun getPTS(): Long {
        var result = System.nanoTime() / 1000L - startPTSUs - offsetPTSUs
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs) {
            val offset: Long = prevOutputPTSUs - result
            offsetPTSUs -= offset
            result += offset
        }
        return result
    }

    private inner class AudioThread(private val audioRecord: AudioRecord) : Thread() {
        private val o = Object()

        @SuppressLint("MissingPermission")
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            var cnt = 0
            try {
                if (isCapturing) {

                    val buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME)
                    var readBytes: Int
                    audioRecord.startRecording()
                    try {
                        while (isCapturing && !isRequestStop && !isEOS) {
                            // read audio data from internal mic
                            buf.clear()
                            readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME)
                            Log.d(
                                TAG,
                                "readBytes :$readBytes"
                            )
                            if (readBytes > 0) {
                                // set audio data to encoder
                                buf.position(readBytes)
                                buf.flip()
                                outputWrapper.writePCM(buf.duplicate())
                                if (recordSampleRate != encodeSampleRate) {
                                    buf.mark()
                                    buf.reset()
                                    ssrcOutBuf.clear()
//                                    val ret = convertor.feedData(buf.array(), readBytes)
//                                    val outByteArray = ByteArray(convertor.convertedSize)
//                                    val size = convertor.receiveConvertedData(outByteArray)
                                    val outByteArray = resampler?.resample(buf.array())
                                    outByteArray?.let { ssrcOutBuf.put(it) }
                                    ssrcOutBuf.flip()
                                    outputWrapper.writeSSRCPCM(ssrcOutBuf.duplicate())
                                    encode(ssrcOutBuf, outByteArray?.size ?: 0, getPTS())
                                } else {
                                    encode(buf, readBytes, getPTS())
                                }
                                cnt++
                            }
                        }
                    } finally {
                        audioRecord.stop()
                    }
                }
            } finally {
                audioRecord.release()
            }
            if (cnt == 0) {
                val buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME)
                var i = 0
                while (isCapturing && i < 5) {
                    buf.position(SAMPLES_PER_FRAME)
                    buf.flip()
                    try {
                        encode(buf, SAMPLES_PER_FRAME, getPTS())
                        outputWrapper.writePCM(buf.duplicate())
                    } catch (e: Exception) {
                        break
                    }
                    synchronized(o) {
                        try {
                            o.wait(50)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    i++
                }
            }
        }
    }
}
