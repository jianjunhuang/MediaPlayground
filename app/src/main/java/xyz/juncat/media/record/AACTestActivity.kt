package xyz.juncat.media.record

import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.juncat.media.LogActivity

open class AACTestActivity : LogActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initActionView(frameLayout: FrameLayout) {
        frameLayout.addView(
            AppCompatButton(this).apply {
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
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource("/sdcard/Android/data/xyz.juncat.videoeditor/cache/cc85de2b-00ca-445e-98ab-57d3441f9da4.aac.aac")
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        log("duration: $duration")
    }
}