package xyz.juncat.media.cut

import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.juncat.media.LogActivity
import xyz.juncat.media.Utils
import java.io.File


class VideoTrimByCodecActivity : LogActivity() {

    override fun initActionView(frameLayout: FrameLayout) {
        frameLayout.addActionButton {
            lifecycleScope.launch {
                val uri = selectMedia("video/*")
                uri.let {
                    if (it == null) {
                        log("uri is null", true)
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        val path = Utils.copyInternal(this@VideoTrimByCodecActivity, it, "trim")
                        val resultPath = File(path.parent, "result.mp4").absolutePath
                        log("start transcode")
                        val start = System.currentTimeMillis()
                        VideoQuickTrim().startTrim(
                            path,
                            resultPath,
                            arrayListOf(
                                LongRange(8 * 1000000, 10 * 1000000)
                            )
                        )
                        log("transcode finish: ${System.currentTimeMillis() - start}")
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            TranscodeWrapperDemo3(
//                                resultPath, arrayOf(
//                                    TailTimer(
//                                        0,
//                                        Int.MAX_VALUE.toLong(),
//                                        path.path
//                                    )
//                                ).toMutableList(),
//                                object: Callback {
//                                    override fun onCall() {
//                                        lifecycleScope.launch {
//                                            log("transcode finish: ${System.currentTimeMillis() - start}")
//                                        }
//                                    }
//
//                                }
//                            ).startTranscode()
//                        }
                    }
                }
            }
        }
    }


    companion object {
        private const val TAG = "VideoTrimByCodecAct"
    }
}