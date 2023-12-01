package xyz.juncat.media

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.children
import xyz.juncat.media.convert.ConvertActivity
import xyz.juncat.media.cut.VideoCutActivity
import xyz.juncat.media.cut.VideoTrimByCodecActivity
import xyz.juncat.media.databinding.ActivityMainBinding
import xyz.juncat.media.edit.EditCutActivity
import xyz.juncat.media.ffmpeg.FFmpegTestPlayGroundActivity
import xyz.juncat.media.frames.FramesExtractActivity
import xyz.juncat.media.ijk.IjkPlayActivity
import xyz.juncat.media.m3u.M3UActivity
import xyz.juncat.media.m3u.MP4ToM3UActivity
import xyz.juncat.media.record.AACTestActivity
import xyz.juncat.media.record.AudioRecordActivity
import xyz.juncat.media.record.screen.RecordActivity
import xyz.juncat.media.record.SSRCActivity
import xyz.juncat.media.record.screen.RecordActivity2
import xyz.juncat.media.videolist.VideoListActivity

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private var clickId: Int = -1
    private val dataIntentMapper = mapOf(
        R.id.btn_extract_music to MusicExtractorActivity::class.java,
        R.id.btn_extract_frames to FramesExtractActivity::class.java,
        R.id.btn_video_list to VideoListActivity::class.java,
        R.id.btn_ijk to IjkPlayActivity::class.java,
        R.id.btn_mp4_to_m3u to MP4ToM3UActivity::class.java,
        R.id.btn_mp4_edit to EditCutActivity::class.java,
        R.id.btn_convert to ConvertActivity::class.java,
        R.id.btn_video_cut to VideoCutActivity::class.java,
    )

    private val intentMapper = mapOf(
        R.id.btn_m3u to M3UActivity::class.java,
        R.id.btn_record to RecordActivity::class.java,
        R.id.btn_audio_record to AudioRecordActivity::class.java,
        R.id.btn_ffmpeg_test to FFmpegTestPlayGroundActivity::class.java,
        R.id.btn_ssrc to SSRCActivity::class.java,
        R.id.btn_aac_test to AACTestActivity::class.java,
        R.id.btn_mediacodec_cut to VideoTrimByCodecActivity::class.java,
        R.id.btn_record_2 to RecordActivity2::class.java,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clearAllCache()
        binding.buttonLayout.children.forEach { child ->
            child.setOnClickListener(this)
        }
    }

    private fun clearAllCache() {
        applicationContext.cacheDir
//        externalCacheDir?.deleteRecursively()
    }

    override fun onClick(v: View?) {
        clickId = v?.id ?: -1
        if (dataIntentMapper.containsKey(clickId)) {
            val targetIntent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "video/mp4"
            }
            startActivityForResult(Intent.createChooser(targetIntent, "select video"), 200)
        } else {
            startActivity(Intent(this, intentMapper[clickId]))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && data != null) {
            val videos = ArrayList<Uri>()
            if (data.data != null) {
                data.data?.let { uri ->
                    videos.add(uri)
                }
            } else if (data.clipData != null) {
                for (index in 0 until (data.clipData?.itemCount ?: 0)) {
                    data.clipData?.getItemAt(index)?.let {
                        videos.add(it.uri)
                    }
                }
            }
            videos.firstOrNull()?.let {
                startIntent(clickId, it)
            }
        }
    }

    private fun startIntent(viewId: Int, videoUri: Uri) {
        val intent = Intent().apply {
            data = videoUri
            dataIntentMapper[viewId]?.let { setClass(this@MainActivity, it) }
        }
        if (viewId > 0) startActivity(intent)
    }
}