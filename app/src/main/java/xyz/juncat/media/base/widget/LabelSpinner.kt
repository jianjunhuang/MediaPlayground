package xyz.juncat.media.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView

class LabelSpinner : LinearLayout {

    private val labelTextView = AppCompatTextView(context).also {

    }

    private val spinner = Spinner(context)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        orientation = HORIZONTAL
        addView(labelTextView)
        addView(spinner)
    }

    fun setLabel(text: String) {
        labelTextView.text = text
    }

    fun setStringArray(stringArray: List<String>) {
        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            stringArray
        )
    }

    var onItemSelectedListener: AdapterView.OnItemSelectedListener? = null
        set(value) {
            spinner.onItemSelectedListener = value
            field = value
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        spinner.isEnabled = enabled
    }

    val selectedItemPosition
        get() = spinner.selectedItemPosition
}