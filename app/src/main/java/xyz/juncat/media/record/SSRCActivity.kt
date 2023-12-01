package xyz.juncat.media.record

import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.juncat.media.LogActivity
import xyz.juncat.media.record.ssrc.SSRC
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SSRCActivity : LogActivity() {

    override fun initActionView(frameLayout: FrameLayout) {
        frameLayout.addView(
            AppCompatButton(this@SSRCActivity).apply {
                text = "start"
                setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        test()
                    }
                }
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        )
    }


    private suspend fun test() {
        val pcmFile = File(externalCacheDir, "/ext_audio/ext.pcm")
        if (!pcmFile.exists()) {
            log("pcm file not exists($pcmFile), go to AudioRecordActivity record first", true)
            return
        }
        val pcmOutFile = File(externalCacheDir, "ext_audio/ext_ssrc.pcm")
        val ins = FileInputStream(pcmFile)
        val ops = FileOutputStream(pcmOutFile)
        log("start ssrc")
        log("44100 -> 16000")
        SSRC(ins, ops, 44100, 16000, 2, 2, 1, Int.MAX_VALUE, 0.0, 0, true)
        log("ssrc finish")
    }
}