package xyz.juncat.media.record.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import xyz.juncat.media.record.screen.config.AudioConfig
import xyz.juncat.media.record.screen.config.VideoConfig

/**
 * foreground service :https://developer.android.com/guide/components/foreground-services
 */
class ScreenRecordService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val channel = NotificationChannelCompat.Builder(
            "ScreenRecord",
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName("ScreenRecordChannel")//if Name is empty, it will throw IllegalArgumentException
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channel.id)
            .setContentTitle("ScreenRecord")
            .build()
        ServiceCompat.startForeground(
            this,
            101,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else 0
        )

    }

    override fun onBind(intent: Intent?): IBinder? {
        return ScreenRecordServiceBinder(this)
    }

    class ScreenRecordServiceBinder(private val service: ScreenRecordService) : Binder() {

        fun startRecord(
            videoConfig: VideoConfig?,
            audioConfig: AudioConfig?,
            mediaProjectionResultCode: Int?,
            mediaProjectionIntent: Intent?
        ) {
            if (mediaProjectionResultCode != null && mediaProjectionIntent != null) {
                val mpm = service.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = mpm.getMediaProjection(mediaProjectionResultCode, mediaProjectionIntent)
            }
            //create video encoder
            //create audio encoder
            //create muxer

        }

        fun stopRecord() {

        }

        fun pauseRecord() {

        }

        fun resumeRecord() {

        }
    }

    companion object {

        const val ACTION_START = "START"

    }
}