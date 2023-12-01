package xyz.juncat.media.record

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.juncat.media.databinding.ActivityScreenRecordBinding
import java.io.File


class RecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenRecordBinding
    private var mediaProjectionManager: MediaProjectionManager? = null
    private val displayMetrics by lazy { DisplayMetrics() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        binding.btnMediaProjection.setOnClickListener {
            recordByMediaProjection()
        }
        binding.btnMediaProjectionStop.setOnClickListener {
            stopService(Intent(this, ProjectionService::class.java))
        }
    }

    private fun recordByMediaProjection() {
        mediaProjectionManager =
            (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager)
        mediaProjectionManager?.apply {
            val captureIntent = this.createScreenCaptureIntent()
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivityForResult(captureIntent, 100)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {

            //delay
            lifecycleScope.launch {
                startForegroundService(Intent(
                    this@RecordActivity, ProjectionService::class.java
                ).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("resultData", data)
                    putExtra("width", displayMetrics.widthPixels)
                    putExtra("height", displayMetrics.heightPixels)
                    putExtra("dpi", displayMetrics.densityDpi)
                })
                delay(200)
            }
        }
    }

    companion object {
        const val TAG = "RecordActivity"
    }

    public class ProjectionService : Service() {

        private var mediaProjectionManager: MediaProjectionManager? = null
        private var mediaProjection: MediaProjection? = null
        private var virtualDisplay: VirtualDisplay? = null
        private val savedParentFile by lazy { File(externalCacheDir, "record") }
        private val savedFile by lazy { File(savedParentFile, "tmp.mp4") }
        private val recorder = H264Recorder()

        override fun onCreate() {
            super.onCreate()
            Log.i(TAG, "onCreate")
            mediaProjectionManager =
                (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager)
            notification()
        }

        private fun notification() {
            Log.i(TAG, "notification: " + Build.VERSION.SDK_INT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val NOTIFICATION_CHANNEL_ID = baseContext.packageName + "_record_video"
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "record_video_channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.enableLights(true)
                channel.lightColor = Color.BLUE
                channel.setDescription("for record video")
                val notificationManager: NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                //Call Start foreground with notification
                val notificationIntent = Intent(
                    this, RecordActivity::class.java
                )
                val pendingIntent: PendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                val notificationBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Starting Service")
                        .setContentText("Starting monitoring service")
                        .setContentIntent(pendingIntent)
                val notification: Notification = notificationBuilder.build()
                startForeground(
                    1001, notification
                )
            }
        }

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            Log.i(TAG, "start command - " + intent.toString())
            try {
                val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
                Log.i(TAG, "onStartCommand: $resultCode")
                val resultData: Intent =
                    intent?.getParcelableExtra("resultData") ?: return super.onStartCommand(
                        intent, flags, startId
                    )

                Log.i(TAG, "onStartCommand: ${resultData.toString()}")
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
                val width = Math.min(intent.getIntExtra("width", 0) ?: 0, 1080)
                val height = Math.min(intent.getIntExtra("height", 0), 1920)
                val dpi = intent.getIntExtra("dpi", 2)
                recorder.init(mediaProjection)
                if (!savedParentFile.exists()) {
                    savedParentFile.mkdirs()
                }
                Log.i(TAG, savedParentFile.absolutePath)
                if (savedFile.exists()) {
                    savedFile.delete()
                }
                val surface = recorder.prepare(width, height, savedFile.absolutePath)
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "MainScreen",
                    width,
                    height,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface,
                    null,
                    null
                )
                recorder.start()
            } catch (t: Throwable) {
                Log.e(TAG, t.stackTraceToString())
            }

            return START_STICKY
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.i(TAG, "onDestroy")
            try {
                recorder.destroy()
            } catch (t: Throwable) {
                Log.e(TAG, t.toString())
            } finally {
                virtualDisplay?.release()
                mediaProjection?.stop()
            }
        }

    }

    interface IRecorder {
        fun init(mediaProjection: MediaProjection?)
        fun prepare(width: Int, height: Int, videoPath: String): Surface
        fun start()
        fun destroy()
    }
}