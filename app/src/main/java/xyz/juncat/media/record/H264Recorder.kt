package xyz.juncat.media.record

import android.media.*
import android.media.projection.MediaProjection
import android.os.Bundle
import android.util.Log
import android.view.Surface
import xyz.juncat.media.record.RecordActivity.Companion.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.experimental.and

class H264Recorder : RecordActivity.IRecorder, Runnable {
        private var videoMediaCodec: MediaCodec? = null
        private var audioMediaCodec: MediaCodec? = null
        private var writer: RandomAccessFile? = null
        private var audioWriter: FileOutputStream? = null
        private var projection: MediaProjection? = null

        private var chunk: ByteArray? = null
        override fun init(mediaProjection: MediaProjection?) {
            videoMediaCodec = MediaCodec.createEncoderByType("video/avc")
            audioMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm")
            projection = mediaProjection
        }

        override fun prepare(width: Int, height: Int, videoPath: String): Surface {
            val parent = File(videoPath).parent
            val file = File(parent, "test.h264")
            if (!file.exists()) {
                file.createNewFile()
            } else {
                file.delete()
                file.createNewFile()
            }
            writer = RandomAccessFile(file, "rw")
            val frameRate = 30
            val videoFormat = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 6000000)//6Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate)
                setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate)
                setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)// 1 seconds between I-frames
            }
            videoMediaCodec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val audioFile = File(parent, "test.aac")
            if (audioFile.exists()) {
                audioFile.delete()
            }
            audioFile.createNewFile()
            audioWriter = FileOutputStream(audioFile)
            val audioFormat = MediaFormat.createAudioFormat(
                "audio/mp4a-latm",
                sampleRate,
                1
            ).apply {
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    64000
                )
                setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            }
            audioMediaCodec?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)


            return videoMediaCodec?.createInputSurface() ?: throw IllegalArgumentException()
        }

        override fun start() {
            Thread(Runnable {
                recordAudio()
            }).start()
            Thread(this).start()
        }

        val sampleRate = 44100

        private fun recordAudio() {
            audioMediaCodec?.start()
            val min_buffer_size = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val SAMPLES_PER_FRAME = 1024
            var buffer_size: Int = SAMPLES_PER_FRAME * 25
            if (buffer_size < min_buffer_size) buffer_size =
                (min_buffer_size / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2

            val audioRecord = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setSampleRate(sampleRate)
                        .build()
                )
                .setBufferSizeInBytes(buffer_size)
                .setAudioPlaybackCaptureConfig(
                    AudioPlaybackCaptureConfiguration.Builder(projection!!)
                        .addMatchingUsage(AudioAttributes.USAGE_ALARM)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build()
                ).build()
            val buf = ByteArray(SAMPLES_PER_FRAME)
            var readBytes = 0
            try {
                audioRecord.startRecording()
                while (isRecording) {
                    readBytes = audioRecord.read(buf, 0, SAMPLES_PER_FRAME)
                    if (readBytes > 0) {
                        val inputBufferIndex = audioMediaCodec?.dequeueInputBuffer(-1) ?: -1
                        if (inputBufferIndex >= 0) {
                            val inputBuffer =
                                audioMediaCodec?.getInputBuffer(inputBufferIndex)?.let {
                                    it.clear()
                                    it.put(buf)
                                    it.limit(buf.size)
                                    audioMediaCodec?.queueInputBuffer(
                                        inputBufferIndex,
                                        0,
                                        readBytes,
                                        System.nanoTime() / 1000L,
                                        0
                                    )
                                }
                        }
                        val info = MediaCodec.BufferInfo()
                        var outputBufferIndex = audioMediaCodec?.dequeueOutputBuffer(info, 0) ?: -1
                        while (outputBufferIndex >= 0) {
                            Log.i(TAG, "recordAudio: presentationTimeUs ${info.presentationTimeUs}")
                            val outputBuffer =
                                audioMediaCodec?.getOutputBuffer(outputBufferIndex)?.let {
                                    it.position(info.offset)
                                    it.limit(info.offset + info.size)
                                    val chunk = ByteArray(info.size + 7)//7 is ADTS size
                                    addADTStoPacket(chunk, chunk.size)
                                    it.get(chunk, 7, info.size)
                                    it.position(info.offset)
                                    audioWriter?.write(chunk)
                                    audioMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                                    outputBufferIndex =
                                        audioMediaCodec?.dequeueOutputBuffer(info, 0) ?: -1
                                }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "recordAudio: ", t)
            } finally {
                audioRecord.stop()
                audioRecord.release()
            }
        }

        private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
            val profile = 2 //AAC LC
            val freqIdx = 4 //44.1KHz
            val chanCfg = 1 //CPE
            // fill in ADTS data
            packet[0] = 0xFF.toByte()
            packet[1] = 0xF9.toByte()
            packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
            packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
            packet[4] = (packetLen and 0x7FF shr 3).toByte()
            packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
            packet[6] = 0xFC.toByte()
        }

        private var isRecording = true
        private var len = 0L
        private var sps_pps: ByteArray? = null
        private var h264SpsPpsData: ByteArray? = null

        override fun run() {
            videoMediaCodec?.start()
            val info = MediaCodec.BufferInfo()
            var timeStamp = System.currentTimeMillis()
            while (isRecording) {
                if (System.currentTimeMillis() - timeStamp >= 1000) {
                    Bundle().apply {
                        putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                        videoMediaCodec?.setParameters(this)
                    }
                    timeStamp = System.currentTimeMillis()
                }
                try {
                    val outputBufferIndex =
                        videoMediaCodec?.dequeueOutputBuffer(info, -1) ?: -1
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoMediaCodec?.outputFormat?.let { format ->
                            var byteBuffer = format.getByteBuffer("csd-0")
                            var sps: ByteArray? = null
                            if (byteBuffer != null) {
                                sps = ByteArray(byteBuffer.capacity())
                                byteBuffer.get(sps)
                            }
                            byteBuffer = format.getByteBuffer("csd-1")
                            var pps: ByteArray? = null
                            if (byteBuffer != null) {
                                pps = ByteArray(byteBuffer.capacity())
                                byteBuffer.get(pps)
                            }
                            if (sps != null && pps != null) {
                                val spsPps = ByteArray(sps.size + pps.size)
                                System.arraycopy(sps, 0, spsPps, 0, sps.size)
                                System.arraycopy(pps, 0, spsPps, sps.size, pps.size)
                                h264SpsPpsData = spsPps
                            }
                        }
                    }
                    if (outputBufferIndex >= 0) {
                        Log.i(TAG, "run: presentationTimeUs -> ${info.presentationTimeUs}")
                        val byteBuffer = videoMediaCodec?.getOutputBuffer(outputBufferIndex)
                        byteBuffer?.let {
                            it.position(info.offset)
                            it.limit(info.offset + info.size)
                            chunk = ByteArray(info.size)
                            it.get(chunk)
                            videoMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                            if (chunk != null && chunk!!.isNotEmpty()) {
                                //防断流黑屏方法2：融合sps和pps，配合format中的每隔1秒请求一次关键帧 I帧
                                if ((chunk!![4] and 0x1f).toInt() == 5) {
                                    h264SpsPpsData?.let {
                                        writer?.run {
                                            seek(length())
                                            write(it)
                                        }
                                    }
                                }
                                writer?.run {
                                    seek(length())
                                    write(chunk)
                                }
                                val frame = when ((chunk!![4] and 0x1f).toInt()) {
                                    7 -> "SPS"
                                    8 -> "PPS"
                                    5 -> "I"
                                    1 -> "P"
                                    else -> "other"
                                }
                                Log.i(TAG, "run: frame -> $frame")
                            }
                        }
                    } else {
                        if (chunk != null && chunk!!.isNotEmpty()) {
                            //防断流黑屏方法2：融合sps和pps，配合format中的每隔1秒请求一次关键帧 I帧
                            if ((chunk!![4] and 0x1f).toInt() == 5) {
                                h264SpsPpsData?.let {
                                    writer?.run {
                                        seek(length())
                                        write(it)
                                    }
                                }
                                writer?.run {
                                    seek(length())
                                    write(chunk)
                                }
                            } else {
                                writer?.run {
                                    seek(length())
                                    write(chunk)
                                }
                            }
                            val frame = when ((chunk!![4] and 0x1f).toInt()) {
                                7 -> "SPS"
                                8 -> "PPS"
                                5 -> "I"
                                1 -> "P"
                                else -> "other"
                            }
                            Log.i(TAG, "run: cache frame -> $frame")
                        }
                    }

//                    if (outputBufferIndex >= 0) {
//                        data = ByteArray(info.size)
//                        byteBuffer?.get(data)
//                        data?.run {
//                            val type = check(this)
//                            if (type == 7 || type == 8) {
//                                sps_pps = ByteArray(info.size)
//                                System.arraycopy(data, 0, sps_pps, 0, info.size)
//                            }
//                        }
//                        writer?.run {
//                            seek(length())
//                            write(data)
//                            videoMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
//                        }
//                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                        Log.i(TAG, "run: format changed")
//                        videoMediaCodec?.outputFormat?.let {
//                            it.setByteBuffer("csd-0", byteBuffer)
//                            info.size = 0
//                        }
//                    } else {
//                        writer?.run {
//                            seek(length())
//                            if (sps_pps != null) {
//                                write(sps_pps)
//                            }
//                        }
//                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "run: ", t)
                }
            }
        }

        private fun check(data: ByteArray): Int {
            var index = 4  // 00 00 00 01
            if (data[2].toInt() == 0X1) { // 00 00 01
                index = 3
            }
            // NALU的数据类型,header 1个字节的后五位
            val naluType = (data[index].and(0x1F)).toInt()
            if (naluType == 7) {
                Log.d(TAG, "SPS")
            } else if (naluType == 8) {
                Log.d(TAG, "PPS")
            } else if (naluType == 5) {
                Log.d(TAG, "IDR")
            } else {
                Log.d(TAG, "非IDR=" + naluType)
            }
            return naluType
        }

        override fun destroy() {
            isRecording = false
            videoMediaCodec?.stop()
            audioMediaCodec?.stop()
        }

    }
