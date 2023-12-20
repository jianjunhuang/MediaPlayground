package xyz.juncat.media.record.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.juncat.media.base.LogActivity
import xyz.juncat.media.base.widget.LabelEditText
import xyz.juncat.media.base.widget.LabelSpinner
import xyz.juncat.media.record.screen.config.AudioConfig
import xyz.juncat.media.record.screen.config.VideoConfig
import java.lang.ref.WeakReference

class RecordActivity2 : LogActivity() {

    private lateinit var recordStarter: RecordStarter
    private val configurationViews = mutableListOf<View>()
    private val fpsModes = listOf(VideoConfig.FPSMode.VFR, VideoConfig.FPSMode.CFR)
    private val bitrateModes = listOf(
        VideoConfig.BitrateMode.VBR,
        VideoConfig.BitrateMode.CQ,
        VideoConfig.BitrateMode.CBR,
        VideoConfig.BitrateMode.CBR_FD
    )
    private val audioSampleRates = listOf(
        AudioConfig.Sample._16000,
        AudioConfig.Sample._44100,
        AudioConfig.Sample._48000,
        AudioConfig.Sample._64000,
        AudioConfig.Sample._88200,
        AudioConfig.Sample._96000
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordStarter = RecordStarter(this)
        recordStarter.init()
    }

    override fun initActionView(frameLayout: FrameLayout) {
        if (configurationViews.isNotEmpty()) return
        val widthEdt = LabelEditText(this).stash {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("720")
            setTitle("width")
        }
        val heightEdt = LabelEditText(this).stash {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("1080")
            setTitle("height")
        }

        val videoBitrateEdt = LabelEditText(this).stash {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("6000")
            setTitle("video bitrate")
        }

        val fpsEdt = LabelEditText(this).stash {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("30")
            setTitle("fps")
        }

        val iFrameIntervalEdt = LabelEditText(this).stash {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("1")
            setTitle("iFrameInterval")
        }

        val fpsMode = LabelSpinner(this).stash {
            setLabel("fps mode")
            setStringArray(fpsModes.map { it.name })
        }

        val bitrateMode = LabelSpinner(this).stash {
            setLabel("bitrate mode")
            setStringArray(bitrateModes.map { it.name })
        }

        val videoConfigTitle = CheckBox(this).stash {
            isChecked = true
            text = "Video Config"
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        val audioConfigTitle = CheckBox(this).stash {
            isChecked = true
            text = "Audio Config"
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        val audioSampleRate = LabelSpinner(this).stash {
            setLabel("sample")
            setStringArray(audioSampleRates.map { it.displayName })
        }

        val audioChannel = LabelSpinner(this).stash {
            setLabel("channel")
            setStringArray(listOf("1", "2"))
        }

        val audioBitrate = LabelEditText(this).stash {
            setHint("64000")
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setTitle("bitrate")
        }

        val startToggle = ToggleButton(this).apply {
            text = "start/stop"
            textOn = "recording"
            textOff = "stopped"
            setOnCheckedChangeListener { buttonView, isChecked ->
                //disable all configure view
                setConfigurable(!isChecked)
                if (isChecked) {
                    //covert all config to config object
                    try {
                        val videoConfig = videoConfigTitle.takeIf { it.isChecked }?.let {
                            VideoConfig(
                                width = widthEdt.getText().toInt(),
                                height = heightEdt.getText().toInt(),
                                bitrate = videoBitrateEdt.getText().toInt(),
                                fps = fpsEdt.getText().toInt(),
                                iFrameInterval = iFrameIntervalEdt.getText().toInt(),
                                fpsMode = fpsModes[fpsMode.selectedItemPosition],
                                bitrateMode = bitrateModes[bitrateMode.selectedItemPosition]
                            )
                        }
                        lifecycleScope.launch {
                            log("video config: $videoConfig")
                        }

                        val audioConfig = audioConfigTitle.takeIf { it.isChecked }?.let {
                            AudioConfig(
                                sample = audioSampleRates[audioSampleRate.selectedItemPosition].value,
                                channel = audioChannel.selectedItemPosition + 1,
                                bitrate = audioBitrate.getText().toInt()
                            )
                        }
                        lifecycleScope.launch {
                            log("audio config: $audioConfig")
                        }
                        recordStarter.start(videoConfig, audioConfig)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        lifecycleScope.launch {
                            log("start failed: ${t.message}")
                        }
                    }

                } else {
                    recordStarter.stop()
//                    stopService(intent)
                }
            }
        }

        val pauseToggle = ToggleButton(this).apply {
            text = "pause/resume"
            textOn = "resumed"
            textOff = "paused"
            setOnCheckedChangeListener { buttonView, isChecked ->

            }
        }

        frameLayout.addView(
            GridLayout(this).apply {
                columnCount = 2
                orientation = GridLayout.HORIZONTAL
                //video
                add(videoConfigTitle)
                add(widthEdt)
                add(heightEdt, 1)
                add(videoBitrateEdt)
                add(fpsEdt, 1)
                add(iFrameIntervalEdt)
                add(fpsMode)
                add(bitrateMode, 1)

                //audio
                add(audioConfigTitle)
                add(audioSampleRate)
                add(audioChannel, 1)
                add(audioBitrate)

                add(startToggle)
                add(pauseToggle, 1)
            }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )

    }

    private fun GridLayout.add(view: View, index: Int = 0) {
        addView(view, GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(index, 1f)
        })
    }

    private inline fun <T : View> T.stash(block: T.() -> Unit): T {
        configurationViews.add(this)
        block()
        return this
    }

    private fun setConfigurable(configurable: Boolean) {
        configurationViews.forEach {
            it.isEnabled = configurable
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}