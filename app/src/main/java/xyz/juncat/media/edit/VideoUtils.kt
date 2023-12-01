package xyz.juncat.media.edit

import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.IOException
import java.nio.ByteBuffer
import android.os.Handler
import android.os.Looper


class VideoUtils {
    companion object {
        /**
         * @param srcPath the path of source video file.
         * @param dstPath the path of destination video file.
         * @param startMs starting time in milliseconds for trimming. Set to
         * negative if starting from beginning.
         * @param endMs end time for trimming in milliseconds. Set to negative if
         * no trimming at the end.
         * @param useAudio true if keep the audio track from the source.
         * @param useVideo true if keep the video track from the source.
         * @throws IOException
         */
        @Throws(IOException::class)
        fun startTrim(
            srcPath: String, dstPath: String,
            startMs: Int, endMs: Int, useAudio: Boolean, useVideo: Boolean,
            listener: Listener
        ) {
            runOnUiThread {
                listener.onStart()
            }
            // Set up MediaExtractor to read from the source.
            val extractor = MediaExtractor()
            extractor.setDataSource(srcPath)
            val trackCount = extractor.trackCount
            // Set up MediaMuxer for the destination.
            val muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            // Set up the tracks and retrieve the max buffer size for selected
            // tracks.
            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = -1
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                var selectCurrentTrack = false
                if (mime!!.startsWith("audio/") && useAudio) {
                    selectCurrentTrack = true
                } else if (mime.startsWith("video/") && useVideo) {
                    selectCurrentTrack = true
                }
                if (selectCurrentTrack) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        bufferSize = if (newSize > bufferSize) newSize else bufferSize
                    }
                }
            }
            if (bufferSize < 0) {
                bufferSize = DEFAULT_BUFFER_SIZE
            }
            // Set up the orientation and starting time for extractor.
            val retrieverSrc = MediaMetadataRetriever()
            retrieverSrc.setDataSource(srcPath)
            val degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )
            if (degreesString != null) {
                val degrees = degreesString.toInt()
                if (degrees >= 0) {
                    muxer.setOrientationHint(degrees)
                }
            }
            if (startMs > 0) {
                extractor.seekTo((startMs * 1000).toLong(), MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
            // Copy the samples from MediaExtractor to MediaMuxer. We will loop
            // for copying each sample and stop when we get to the end of the source
            // file or exceed the end time of the trimming.
            val offset = 0
            var trackIndex: Int
            val dstBuf = ByteBuffer.allocate(bufferSize)
            val bufferInfo = BufferInfo()
            val totalTimeMs = endMs - startMs
            try {
                muxer.start()
                while (true) {
                    bufferInfo.offset = offset
                    bufferInfo.size = extractor.readSampleData(dstBuf, offset)
                    if (bufferInfo.size < 0) {
                        runOnUiThread {
                            listener.onComplete()
                        }
                        bufferInfo.size = 0
                        break
                    } else {
                        bufferInfo.presentationTimeUs = extractor.sampleTime
                        if (endMs > 0 && bufferInfo.presentationTimeUs > endMs * 1000) {
                            runOnUiThread {
                                listener.onComplete()
                            }
                            break
                        } else {
                            bufferInfo.flags = extractor.sampleFlags
                            trackIndex = extractor.sampleTrackIndex
                            muxer.writeSampleData(
                                indexMap[trackIndex]!!, dstBuf,
                                bufferInfo
                            )
                            runOnUiThread {
                                listener.onProgress((bufferInfo.presentationTimeUs / 1000 - startMs).toFloat() / totalTimeMs)
                            }
                            extractor.advance()
                        }
                    }
                }
                muxer.stop()
            } catch (e: IllegalStateException) {
                runOnUiThread {
                    listener.onError("The source video file is malformed")
                }
            } finally {
                muxer.release()
            }
            return
        }
    }

    interface Listener {
        fun onStart()
        fun onProgress(value: Float)
        fun onComplete()
        fun onError(message: String)
    }
}

private val mHandler = Handler(Looper.getMainLooper())

fun runOnUiThread(closure: () -> Unit) {
    mHandler.post {
        closure()
    }
}