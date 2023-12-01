package xyz.juncat.media.record.screen

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.ToggleButton
import xyz.juncat.media.base.LogActivity
import xyz.juncat.media.base.widget.LabelEditText

class RecordActivity2 : LogActivity() {
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

            }
        }

        val pauseToggle = ToggleButton(this).apply {
            text = "pause/resume"
            textOn = "resumed"
            textOff = "paused"
            setOnCheckedChangeListener { buttonView, isChecked ->

            }
        }

        val fpsMode = Spinner(this).apply {
            prompt = "fps mode"
            setAdapter(
                ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("VFR", "CFR")
                )
            )
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

        val bitrateMode = Spinner(this).apply {
            prompt = "bitrate mode"
            setAdapter(
                ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("CQ", "VBR", "CBR", "CBR-FD")
                )
            )
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
        frameLayout.addView(
            GridLayout(this).apply {
                columnCount = 2
                orientation = GridLayout.HORIZONTAL
                addView(widthEdt, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 1f)
                })
                addView(heightEdt, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(1, 1f)
                })
                addView(videoBitrateEdt, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 1f)
                })
                addView(fpsEdt, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(1, 1f)
                })
                addView(iFrameIntervalEdt, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 1f)
                })
                addView(fpsMode, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 1f)
                })
                addView(bitrateMode, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(1, 1f)
                })
                addView(startToggle, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 1f)
                })
                addView(pauseToggle, GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(1, 1f)
                })
            }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )

    }
}