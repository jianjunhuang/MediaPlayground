package xyz.juncat.media.record.screen.config

data class AudioConfig(
    val sample: Int,
    val channel: Int,
    val bitrate: Int
) {
    enum class Sample(
        val displayName: String,
        val value: Int
    ) {
        _44100("44.1kHz", 44100),
        _16000("16kHz", 16000),
        _48000("48kHz", 48000),
        _64000("64kHz", 64000),
        _88200("88.2kHz", 88200),
        _96000("96kHz", 96000),
    }
}