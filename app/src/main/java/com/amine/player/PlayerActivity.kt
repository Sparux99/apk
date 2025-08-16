package com.amine.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.vkay94.dtpv.DoubleTapPlayerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: DoubleTapPlayerView
    private var player: ExoPlayer? = null
    private var videoUri: Uri? = null

    // ▼▼▼ حفظ مكان التوقف
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        videoUri = intent.data
    }

    private fun initializePlayer() {
        if (videoUri == null) return

        // استعادة مكان التوقف
        restorePlayerState()

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val mediaItem = MediaItem.fromUri(videoUri!!)
            exoPlayer.setMediaItem(mediaItem)

            // إعداد seek increments للـ gestures
            exoPlayer.seekForwardIncrement = 10000L // 10 ثواني
            exoPlayer.seekBackIncrement = 10000L    // 10 ثواني

            // الانتقال لمكان التوقف
            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            savePlayerState(exoPlayer)
            exoPlayer.release()
        }
        player = null
    }

    private fun savePlayerState(exoPlayer: ExoPlayer) {
        playbackPosition = exoPlayer.currentPosition
        currentItem = exoPlayer.currentMediaItemIndex
        playWhenReady = exoPlayer.playWhenReady

        val prefs = getSharedPreferences("PlayerPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("playbackPosition_${videoUri.toString()}", playbackPosition)
            .apply()
    }

    private fun restorePlayerState() {
        val prefs = getSharedPreferences("PlayerPrefs", Context.MODE_PRIVATE)
        playbackPosition = prefs.getLong("playbackPosition_${videoUri.toString()}", 0L)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}
