package xyz.juncat.media.videolist

import android.net.Uri
import kotlin.random.Random

object VideoDataFactory {

    fun genVideoListData(uri: Uri): List<VideoListData> {
        val dataList = ArrayList<VideoListData>()
        val aspectRatios = floatArrayOf(1f, 4f / 3f, 16f / 9f)
        repeat(20) {
            val count = Random.nextInt(2, 10)
            val videos = ArrayList<VideoData>()
            repeat(count) {
                val ratiosIndex = Random.nextInt(0, aspectRatios.size - 1)
                videos.add(VideoData(uri, aspectRatios[ratiosIndex]))
            }
            dataList.add(VideoListData(it.toString(), videos))
        }
        return dataList
    }
}