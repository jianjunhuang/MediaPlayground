package xyz.juncat.media.decode

import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.juncat.media.base.LogActivity
import java.io.FileOutputStream
import java.io.IOException


class DecoderActivity : LogActivity() {
    override fun initActionView(frameLayout: FrameLayout) {
        frameLayout.addActionButton {
            lifecycleScope.launch(Dispatchers.IO) {
                val videoUri = intent.data
                if (videoUri == null) {
                    log("video uri is null")
                    return@launch
                }

                var width = 0
                var height = 0
                var frameRate = 0
                var bitRate = 0
                var colorFormat: Int? = null

                val videoExtractor = MediaExtractor().apply {
                    setDataSource(this@DecoderActivity, videoUri, null)
                    for (index in 0 until trackCount) {
                        val format = getTrackFormat(index)
                        val type = format.getString(MediaFormat.KEY_MIME)
                        if (type?.startsWith("video") == true) {
                            width = format.getInteger(MediaFormat.KEY_WIDTH)
                            height = format.getInteger(MediaFormat.KEY_HEIGHT)
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                            bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE)
//                            colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
                            selectTrack(index)
                            log("width: $width, height: $height, frameRate: $frameRate, bitRate: $bitRate, colorFormat: $colorFormat")
                            break
                        }
                    }

                }
                val format = MediaFormat.createVideoFormat("video/avc", width, height)
                format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                val decoder = MediaCodec.createDecoderByType("video/avc")
                decoder.configure(format, null, null, 0)
                decoder.start()

                var frame = 0
                val bufferInfo = MediaCodec.BufferInfo()
                while (true) {
                    val inputIndex = decoder.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val sampleSize = videoExtractor.readSampleData(inputBuffer!!, 0)
                        log("sampleSize: $sampleSize")
                        if (sampleSize < 0) {
                            log("send EOS")
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            break
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                videoExtractor.sampleTime,
                                videoExtractor.sampleFlags
                            )
                            videoExtractor.advance()
                        }
                    }

                    val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    log("outputIndex: $outputIndex")
                    if (outputIndex >= 0) {
                        val image = decoder.getOutputImage(outputIndex)

                        image?.let { it1 ->
                            val fileName = externalCacheDir!!.path + String.format(
                                "/frame_%05d_I420_%dx%d.yuv",
                                frame++,
                                width,
                                height
                            )
                            dumpFile(fileName, getDataFromImage(image, COLOR_FormatI420))
//                            getDataFromImage(image, COLOR_FormatI420)
                        }
                        image?.close()
//                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
//                        val offset = bufferInfo.offset
//                        val size = bufferInfo.size
//                        outputBuffer?.get(yPlane, 0, size)
//                        outputBuffer?.get(uPlane, 0, size / 4)
//                        outputBuffer?.get(vPlane, 0, size / 4)

//                        log("yPlane: ${yPlane.size}, uPlane: ${uPlane.size}, vPlane: ${vPlane.size}")
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        log("EOS")
                        break
                    }
                }
                decoder.stop()
                videoExtractor.release()
                decoder.release()
            }
        }
    }

    private val COLOR_FormatI420 = 1
    private val COLOR_FormatNV21 = 2

    private val FILE_TypeI420 = 1
    private val FILE_TypeNV21 = 2
    private val FILE_TypeJPEG = 3
    private fun isImageFormatSupported(image: Image): Boolean {
        val format = image.format
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
        }
        return false
    }

    private suspend fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
        if (!isImageFormatSupported(image)) {
            throw RuntimeException("can't convert Image to byte array, format " + image.format)
        }
        val crop = image.getCropRect()
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        if (VERBOSE) log("get data from " + planes.size + " planes")
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }

                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }

                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            if (VERBOSE) {
                log("pixelStride $pixelStride")
                log("rowStride $rowStride")
                log("width $width")
                log("height $height")
                log("buffer size " + buffer.remaining())
            }
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        if (i > 0) {
                            data[channelOffset] = 0x80.toByte()
                        } else {
                            data[channelOffset] = rowData[col * pixelStride]
                        }
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            if (VERBOSE) log("Finished reading data from plane $i")
        }
        return data
    }

    private fun dumpFile(fileName: String, data: ByteArray) {
        val outStream: FileOutputStream = try {
            FileOutputStream(fileName)
        } catch (ioe: IOException) {
            throw java.lang.RuntimeException("Unable to create output file $fileName", ioe)
        }
        try {
            outStream.write(data)
            outStream.close()
        } catch (ioe: IOException) {
            throw java.lang.RuntimeException("failed writing data to file $fileName", ioe)
        }
    }

    companion object {

        private const val VERBOSE = true
        private const val TAG = "DecoderActivity"
    }
}