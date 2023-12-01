package xyz.juncat.media.player

import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.MediaInfo
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import java.io.FileDescriptor

class SimpleExoMediaPlayer(context: Context) : IMediaPlayer {

    private var player: SimpleExoPlayer = SimpleExoPlayer.Builder(context).build()

    override fun setDisplay(p0: SurfaceHolder?) {
        player.setVideoSurfaceHolder(p0)
    }

    override fun setDataSource(p0: Context?, p1: Uri?) {
        p1?.let {
            val mediaItem = MediaItem.fromUri(p1)
            player.setMediaItem(mediaItem)
        }
    }

    override fun setDataSource(p0: Context?, p1: Uri?, p2: MutableMap<String, String>?) {
        p1?.let {
            val mediaItem = MediaItem.fromUri(p1)
            player.setMediaItem(mediaItem)
        }

    }

    override fun setDataSource(p0: FileDescriptor?) {

    }

    override fun setDataSource(p0: String?) {

    }

    override fun setDataSource(p0: IMediaDataSource?) {

    }

    override fun getDataSource(): String {
        return ""
    }

    override fun prepareAsync() {
        player.playWhenReady = true
        player.prepare()
    }

    override fun start() {
        player.play()
    }

    override fun stop() {
        player.stop()
        player.release()
        player.setVideoSurface(null)
    }

    override fun pause() {
        player.pause()
    }

    override fun setScreenOnWhilePlaying(p0: Boolean) {

    }

    override fun getVideoWidth(): Int {
        return player.videoSize.width
    }

    override fun getVideoHeight(): Int {
        return player.videoSize.height
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun seekTo(p0: Long) {
        player.seekTo(p0)
    }

    override fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    override fun getDuration(): Long {
        return player.duration
    }

    override fun release() {
        player.release()
    }

    override fun reset() {
    }

    override fun setVolume(p0: Float, p1: Float) {
        player.volume = p0
    }

    override fun getAudioSessionId(): Int {
        return player.audioSessionId
    }

    override fun getMediaInfo(): MediaInfo {
        val info = MediaInfo()
        return info
    }

    override fun setLogEnabled(p0: Boolean) {

    }

    override fun isPlayable(): Boolean {
        return true
    }

    override fun setOnPreparedListener(p0: IMediaPlayer.OnPreparedListener?) {

    }

    override fun setOnCompletionListener(p0: IMediaPlayer.OnCompletionListener?) {

    }

    override fun setOnBufferingUpdateListener(p0: IMediaPlayer.OnBufferingUpdateListener?) {

    }

    override fun setOnSeekCompleteListener(p0: IMediaPlayer.OnSeekCompleteListener?) {

    }

    override fun setOnVideoSizeChangedListener(p0: IMediaPlayer.OnVideoSizeChangedListener?) {

    }

    override fun setOnErrorListener(p0: IMediaPlayer.OnErrorListener?) {

    }

    override fun setOnInfoListener(p0: IMediaPlayer.OnInfoListener?) {

    }

    override fun setOnTimedTextListener(p0: IMediaPlayer.OnTimedTextListener?) {

    }

    override fun setAudioStreamType(p0: Int) {

    }

    override fun setKeepInBackground(p0: Boolean) {

    }

    override fun getVideoSarNum(): Int {
        return 0
    }

    override fun getVideoSarDen(): Int {
        return 0
    }

    override fun setWakeMode(p0: Context?, p1: Int) {

    }

    override fun setLooping(p0: Boolean) {
        player.repeatMode = if (p0) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    override fun isLooping(): Boolean {
        return player.isLoading
    }

    override fun getTrackInfo(): Array<ITrackInfo> {
        return emptyArray()
    }

    override fun setSurface(p0: Surface?) {
        player.setVideoSurface(p0)
    }
}