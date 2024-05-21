package info.sensors.bernard.blackoutschedule

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder


class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.bg_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
        return START_STICKY
    }
    fun setMusicVolume(volume: Int) {
        if (mediaPlayer != null) {
            val volumeLevel = volume / 100f
            mediaPlayer!!.setVolume(volumeLevel, volumeLevel)
        }
    }
    fun play() {
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.pause()
        mediaPlayer?.release()
    }
}