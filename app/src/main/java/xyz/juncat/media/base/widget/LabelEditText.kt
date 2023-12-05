package xyz.juncat.media.base.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import xyz.juncat.media.R

class LabelEditText : LinearLayout {

    var editable: Boolean = true
        set(value) {
            field = value
            editText.isEnabled = field
        }

    private val desTextView = AppCompatTextView(context).apply {
        this@LabelEditText.addView(
            this,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    private val editText = AppCompatEditText(context).apply {
        this@LabelEditText.addView(
            this,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        orientation = HORIZONTAL
        context.obtainStyledAttributes(attrs, R.styleable.DesEditText).use {
            val title = it.getString(R.styleable.DesEditText_title)
            val hint = it.getString(R.styleable.DesEditText_android_hint)
            val inputType =
                it.getInt(R.styleable.DesEditText_android_inputType, EditorInfo.TYPE_CLASS_NUMBER)
            desTextView.text = title
            editText.inputType = inputType
            editText.hint = hint
        }

    }


    fun getText(): String {
        val text = editText.text?.toString()
        if (TextUtils.isEmpty(text)) {
            return editText.hint?.toString() ?: ""
        }
        return text?: ""
    }

    fun setHint(hint: String) {
        editText.setHint(hint)
    }

    fun setTitle(title: String) {
        desTextView.text = title
    }

    /**
     * @see EditorInfo
     */
    fun setInputType(inputType: Int) {
        editText.inputType = inputType
    }

    override fun setEnabled(enable: Boolean) {
        super.setEnabled(enable)
        editText.isEnabled = enable
    }
}