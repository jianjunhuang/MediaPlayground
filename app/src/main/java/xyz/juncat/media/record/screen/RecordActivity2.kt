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
import xyz.juncat.media.base.LogActivity
import xyz.juncat.media.base.widget.LabelEditText
import xyz.juncat.media.base.widget.LabelSpinner

class RecordActivity2 : LogActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun initActionView(frameLayout: FrameLayout) {
        val widthEdt = LabelEditText(this).apply {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("720")
            setTitle("width")
        }
        val heightEdt = LabelEditText(this).apply {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("1080")
            setTitle("height")
        }

        val videoBitrateEdt = LabelEditText(this).apply {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("6000")
            setTitle("video bitrate")
        }

        val fpsEdt = LabelEditText(this).apply {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("30")
            setTitle("fps")
        }

        val iFrameIntervalEdt = LabelEditText(this).apply {
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setHint("1")
            setTitle("iFrameInterval")
        }

        val startToggle = ToggleButton(this).apply {
            text = "start/stop"
            textOn = "recording"
            textOff = "stopped"
            setOnCheckedChangeListener { buttonView, isChecked ->
                //TODO check audio permission
                if (isChecked) {

                } else {
                    stopService(intent)
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

        val fpsMode = LabelSpinner(this).apply {
            setLabel("fps mode")
            setStringArray(listOf("VFR", "CFR"))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

            }
        }

        val bitrateMode = LabelSpinner(this).apply {
            setLabel("bitrate mode")
            setStringArray(listOf("VBR", "CQ", "CBR", "CBR-FD"))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

            }
        }

        val videoConfigTitle = CheckBox(this).apply {
            isChecked = true
            text = "Video Config"
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        val audioConfigTitle = CheckBox(this).apply {
            isChecked = true
            text = "Audio Config"
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        val audioSampleRate = LabelSpinner(this).apply {
            setLabel("sample")
            setStringArray(listOf("16kHz", "44.1kHz", "48kHz", "64kHz", "88.2kHz", "96kHz"))
        }

        val audioChannel = LabelSpinner(this).apply {
            setLabel("channel")
            setStringArray(listOf("1", "2"))
        }

        val audioBitrate = LabelEditText(this).apply {
            setHint("64000")
            setInputType(EditorInfo.TYPE_CLASS_NUMBER)
            setTitle("bitrate")
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
}