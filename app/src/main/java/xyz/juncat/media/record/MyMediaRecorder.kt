package xyz.juncat.media.record

import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface
import xyz.juncat.media.record.RecordActivity.Companion.TAG

class MyMediaRecorder() : RecordActivity.IRecorder {
        private var mediaRecorder: MediaRecorder? = null
        override fun init(mediaProjection: MediaProjection?) {
            //init recorder
            mediaRecorder = MediaRecorder()
        }

        override fun prepare(width: Int, height: Int, videoPath: String): Surface {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT)

                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)

                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                setOutputFile(videoPath)
                Log.i(TAG, "width: $width, height: $height")
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(8388608)

                prepare()
            }
            return mediaRecorder?.surface ?: throw RuntimeException("surface is null")
        }

        override fun start() {
            mediaRecorder?.start()
        }

        override fun destroy() {
            try {
                mediaRecorder?.stop()
            } catch (t: Throwable) {
                Log.e(TAG, t.stackTraceToString())
            } finally {
                mediaRecorder?.reset()
            }
        }


    }
