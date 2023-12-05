package xyz.juncat.media.record.screen

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import xyz.juncat.media.record.screen.config.AudioConfig
import xyz.juncat.media.record.screen.config.VideoConfig
import java.lang.IllegalArgumentException

class RecordStarter(private val activity: RecordActivity2) {

    private var recordBinder: ScreenRecordService.ScreenRecordServiceBinder? = null
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<String>
    private var audioConfig: AudioConfig? = null
    private var videoConfig: VideoConfig? = null

    fun init() {
        permissionRequestLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    start(videoConfig, audioConfig)
                }
            }
        mediaProjectionLauncher =
            activity.registerForActivityResult(object : ActivityResultContract<Intent, Intent>() {
                override fun createIntent(context: Context, input: Intent): Intent {
                    return input
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Intent {
                    return intent ?: Intent()
                }

            }) {
                start(videoConfig, audioConfig, it)
            }
    }

    fun start(videoConfig: VideoConfig?, audioConfig: AudioConfig?) {
        if (audioConfig == null && videoConfig == null) {
            throw IllegalArgumentException("audioConfig and videoConfig can't be null at the same time")
        }
        this.audioConfig = audioConfig
        this.videoConfig = videoConfig
        if (audioConfig != null) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                permissionRequestLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
        }

        if (videoConfig != null) {
            val projectionManager =
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun start(
        videoConfig: VideoConfig?,
        audioConfig: AudioConfig?,
        mediaProjectionIntent: Intent?
    ) {
        val serviceIntent = Intent(activity, ScreenRecordService::class.java)
        activity.bindService(serviceIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                recordBinder = service as? ScreenRecordService.ScreenRecordServiceBinder
                recordBinder?.startRecord(videoConfig, audioConfig, mediaProjectionIntent)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }

        }, Context.BIND_IMPORTANT)
    }
}