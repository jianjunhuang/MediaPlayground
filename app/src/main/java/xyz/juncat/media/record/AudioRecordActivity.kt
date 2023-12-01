package xyz.juncat.media.record

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import xyz.juncat.media.databinding.ActivityAudioRecordBinding

class AudioRecordActivity : Activity() {

    private lateinit var binding: ActivityAudioRecordBinding
    private val encoder by lazy {
        MyAudioEncoder(
            externalCacheDir?.absolutePath + "/ext_audio",
            44100,
            2,
            16000,
            1
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnStartRecord.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                encoder.stop()
                binding.btnPauseRecord.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 232)
                }
                return@setOnCheckedChangeListener
            }

            encoder.prepare()
            encoder.start()
        }
        binding.btnPauseRecord.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!binding.btnStartRecord.isChecked) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                encoder.pause()
            } else {
                encoder.resume()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 232 && resultCode == Activity.RESULT_OK &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.btnStartRecord.isChecked = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        encoder.release()
    }
}