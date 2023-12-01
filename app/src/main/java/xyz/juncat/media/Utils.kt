package xyz.juncat.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class Utils {

    companion object {

        fun getVideoDuration(path: String): Long {
            val mediaRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(path)
            return try {
                (mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong() ?: 0L)
            } catch (t: Throwable) {
                0
            } finally {
                mediaRetriever.release()
            }
        }

        fun copyInternal(context: Context, uri: Uri, folderName: String? = null): File {
            val internalFile = File(
                if (folderName == null) context.externalCacheDir else File(
                    context.externalCacheDir,
                    folderName
                ).apply {
                    if (!exists()) {
                        mkdir()
                    }
                },
                "${System.currentTimeMillis()}.mp4"
            )
            //copy to internal ,case image2 can not handle SAF uri
            //https://github.com/tanersener/ffmpeg-kit/issues/39
            if (!internalFile.exists()) {
                val ins = context.contentResolver.openInputStream(uri)
                val ops = FileOutputStream(internalFile)
                val byteArray = ByteArray(1024)
                var len = 0
                while (ins?.read(byteArray)?.let {
                        len = it
                        len > 0
                    } == true
                ) {
                    ops.write(
                        byteArray, 0, len
                    )
                }
                ops.flush()
            }
            return internalFile
        }
    }

}