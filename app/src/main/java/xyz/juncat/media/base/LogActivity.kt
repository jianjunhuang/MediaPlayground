package xyz.juncat.media.base

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.jianjun.base.ext.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

abstract class LogActivity : AppCompatActivity() {

    private lateinit var logView: AppCompatTextView
    private var contentCallback: ((Uri?) -> Unit)? = null
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            contentCallback?.invoke(uri)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            logView = AppCompatTextView(this@LogActivity).apply {
                setPadding(16.dp.toInt(), 16.dp.toInt(), 16.dp.toInt(), 16.dp.toInt())
            }
            addView(
                ScrollView(this@LogActivity).apply {
                    addView(
                        logView,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                },
                LinearLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(
                FrameLayout(this@LogActivity).apply {
                    initActionView(this)
                }, LinearLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )

        })
    }

    abstract fun initActionView(frameLayout: FrameLayout)

    protected suspend fun log(text: String, error: Boolean = false, color: Int? = null) {
        logView.appendL(text, error, color)
    }

    private suspend fun AppCompatTextView.appendL(
        text: String,
        error: Boolean = false,
        color: Int? = null
    ) {
        withContext(Dispatchers.Main) {
            if (!error) {
                if (color != null) {
                    append(SpannableStringBuilder(text + "\n").apply {
                        setSpan(
                            ForegroundColorSpan(color),
                            0,
                            text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    })
                } else {
                    append(text + "\n")
                }
            } else {
                append(SpannableStringBuilder(text + "\n").apply {
                    setSpan(
                        ForegroundColorSpan(Color.RED),
                        0,
                        text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                })
            }
        }
    }

    protected suspend fun selectMedia(mimeType: String): Uri? {
        return suspendCancellableCoroutine {
            contentCallback = { uri ->
                it.resume(uri)
            }
            getContent.launch(mimeType)
        }
    }

    protected fun FrameLayout.addActionButton(onClickListener: View.OnClickListener) {
        addView(
            AppCompatButton(context).apply {
                text = "start"
                setOnClickListener(onClickListener)
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        )
    }
}