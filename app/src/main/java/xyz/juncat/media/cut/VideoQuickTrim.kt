package xyz.juncat.media.cut

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

class VideoQuickTrim {

    private var videoDecoder: MediaCodec? = null
    private var audioDecoder: MediaCodec? = null

    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null

    private var videoExtractor: MediaExtractor? = null
    private var audioExtractor: MediaExtractor? = null

    suspend fun startTrim(
        path: File,
        resultPath: String,
        pendingCutRange: ArrayList<LongRange>
    ) {
        try {


            videoExtractor = MediaExtractor().apply { setDataSource(path.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(path.absolutePath) }
            val muxer = MediaMuxer(resultPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            //find video track
            val videoTrackIndex = videoExtractor?.let { selectTrack(it, "video/") } ?: -1
            val aacTrackIndex = audioExtractor?.let { selectTrack(it, "audio/") } ?: -1
            if (videoTrackIndex < 0 && aacTrackIndex < 0) {
                return
            }

            //start muxer
            val videoFormat = videoTrackIndex.takeIf { it >= 0 }
                ?.let { videoExtractor?.getTrackFormat(videoTrackIndex) }
            val videoTrack = if (videoFormat != null) {
                muxer.addTrack(videoFormat)
            } else null

            val audioFormat = audioExtractor?.getTrackFormat(aacTrackIndex)
            val audioTrack = if (audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else null
            muxer.start()

            if ((videoTrack ?: -1) < 0 && (audioTrack ?: -1) < 0) {
                return
            }

            //muxer video
            if (videoTrackIndex >= 0 && videoFormat != null && videoTrack != null) {
                val videoBufferInfo = MediaCodec.BufferInfo()
                val videoBuffer = ByteBuffer.allocate(1024 * 1024)
                var videoSampleSize = 0
                var offsetPTS = 0L
                var lastPTS = 0L
                while (true) {
                    videoSampleSize = videoExtractor?.readSampleData(videoBuffer, 0) ?: -1
                    if (videoSampleSize < 0) {
                        break
                    }
                    videoBufferInfo.offset = 0
                    videoBufferInfo.size = videoSampleSize
                    videoExtractor?.sampleFlags?.let {
                        videoBufferInfo.flags = it
                    }
                    videoExtractor?.sampleTime?.let {
                        videoBufferInfo.presentationTimeUs = it
                    }
                    if (pendingCutRange.isNotEmpty()) {
                        val currentPTS = videoBufferInfo.presentationTimeUs
                        val range = pendingCutRange.firstOrNull { it.contains(currentPTS) }
                        log("currentPTS: $currentPTS, range: $range")
                        if (range != null) {
                            offsetPTS += (videoBufferInfo.presentationTimeUs - lastPTS)
                            log("offsetPTS")
                        } else {
                            muxer.writeSampleData(videoTrack, videoBuffer, videoBufferInfo.apply {
                                presentationTimeUs -= offsetPTS
                            })
                        }
                    }
                    lastPTS = videoExtractor?.sampleTime ?: 0L
                    videoExtractor?.advance()
                }
            }

            //muxer audio
            if (audioExtractor != null && aacTrackIndex >= 0 && audioFormat != null && audioTrack != null) {
                //get frame
                val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                val audioBufferInfo = MediaCodec.BufferInfo()
                var audioSampleSize = 0
                var offsetPTS = 0L
                var lastPTS = 0L
                while (true) {
                    audioSampleSize = audioExtractor?.readSampleData(audioBuffer, 0) ?: -1
                    if (audioSampleSize < 0) {
                        break
                    }
                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = audioSampleSize
                    audioExtractor?.sampleFlags?.let {
                        audioBufferInfo.flags = it
                    }
                    audioExtractor?.sampleTime?.let {
                        audioBufferInfo.presentationTimeUs = it
                    }
                    if (pendingCutRange.isNotEmpty()) {
                        val currentPTS = audioBufferInfo.presentationTimeUs
                        val range = pendingCutRange.firstOrNull { it.contains(currentPTS) }
                        if (range != null) {
                            offsetPTS += (audioBufferInfo.presentationTimeUs - lastPTS)
                        } else {
                            muxer.writeSampleData(audioTrack, audioBuffer, audioBufferInfo.apply {
                                presentationTimeUs -= offsetPTS
                            })
                        }
                    }
                    lastPTS = audioExtractor?.sampleTime ?: 0L
                    audioExtractor?.advance()
                }
            }

            //stop
            videoExtractor?.release()
            audioExtractor?.release()
            muxer.stop()
            muxer.release()
        } catch (t: Throwable) {
            Log.e(TAG, "muxer: ", t)
        }

    }

    @Throws(IOException::class)
    private suspend fun doExtract(
        extractor: MediaExtractor, trackIndex: Int, decoder: MediaCodec
    ) {
        val TIMEOUT_USEC = 10000
        val decoderInputBuffers = decoder.inputBuffers
        val info = MediaCodec.BufferInfo()
        var inputChunk = 0
        var decodeCount = 0
        var frameSaveTime: Long = 0
        var outputDone = false
        var inputDone = false
        while (!outputDone) {
            log("loop")

            // Feed more data to the decoder.
            if (!inputDone) {
                val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (inputBufIndex >= 0) {
                    val inputBuf = decoderInputBuffers[inputBufIndex]
                    // Read the sample data into the ByteBuffer.  This neither respects nor
                    // updates inputBuf's position, limit, etc.
                    val chunkSize = extractor.readSampleData(inputBuf, 0)
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                        log("sent input EOS")
                    } else {
                        if (extractor.sampleTrackIndex != trackIndex) {
                            log(
                                "WEIRD: got sample from track " + extractor.sampleTrackIndex + ", expected " + trackIndex,
                                true
                            )
                        }
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /*flags*/
                        )
                        log(
                            "submitted frame " + inputChunk + " to dec, size=" + chunkSize
                        )
                        inputChunk++
                        extractor.advance()
                    }
                } else {
                    log("input buffer not available")
                }
            }
            if (!outputDone) {
                val decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC.toLong())
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    log("no output from decoder available")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    log("decoder output buffers changed")
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    log("decoder output format changed: $newFormat")
                } else if (decoderStatus < 0) {
                    log("unexpected result from decoder.dequeueOutputBuffer: $decoderStatus")
                } else { // decoderStatus >= 0
                    log(
                        "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")"
                    )
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        log("output EOS")
                        outputDone = true
                    }
                    val doRender = info.size != 0

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                    // that the texture will be available before the call returns, so we
                    // need to wait for the onFrameAvailable callback to fire.
                    decoder.releaseOutputBuffer(decoderStatus, doRender)
                    if (doRender) {
                        log("awaiting decode of frame $decodeCount")
                        //todo
                        decodeCount++
                    }
                }
            }
        }
    }

    private fun selectTrack(extractor: MediaExtractor, mime: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (trackMime.startsWith(mime)) {
                extractor.selectTrack(i)
                return i
            }
        }
        return -1
    }

    private fun log(msg: String, error: Boolean = false) {
        if (error) {
            Log.e(TAG, msg)
        } else {
            Log.i(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "VideoQuickTrim"
    }

}