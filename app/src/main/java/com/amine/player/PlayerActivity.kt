package com.amine.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var videoUri: Uri? = null

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

        restorePlayerState()

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            val mediaItem = MediaItem.fromUri(videoUri!!)
            exoPlayer.setMediaItem(mediaItem)

            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()

            playerView.player = exoPlayer
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
        prefs.edit().putLong("playbackPosition_${videoUri.toString()}", playbackPosition).apply()
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
