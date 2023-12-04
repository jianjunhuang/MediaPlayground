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

//                activity.bindService(it, object : ServiceConnection {
//                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                        recordBinder = service as? ScreenRecordService.ScreenRecordServiceBinder
//                    }
//
//                    override fun onServiceDisconnected(name: ComponentName?) {
//                    }
//
//                }, Context.BIND_IMPORTANT)
            }
    }

    fun start(videoConfig: VideoConfig?, audioConfig: AudioConfig?) {
        if (audioConfig != null) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                permissionRequestLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            return
        }

        if (videoConfig != null) {
            val projectionManager =
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }
}