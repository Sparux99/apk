package com.amine.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.vkay94.dtpv.DoubleTapPlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: DoubleTapPlayerView
    private var videoUri: Uri? = null

    // متغيرات لحفظ مكان التوقف
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
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUri!!)
            exoPlayer.setMediaItem(mediaItem)

            // استخدام متغيرات مباشرة بدل reassignment لـ val
            exoPlayer.seekForwardIncrement = 10000L // 10 ثواني
            exoPlayer.seekBackIncrement = 10000L // 10 ثواني

            exoPlayer.prepare()

            // تفعيل الإيماءات من DoubleTapPlayerView
            playerView.setPlayer(exoPlayer)

            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            savePlayerState(exoPlayer)
            exoPlayer.release()
        }
        player = null
    }

    private fun savePlayerState(exoPlayer: Player) {
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
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (player == null) initializePlayer()
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
package com.amine.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.vkay94.dtpv.DoubleTapPlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: DoubleTapPlayerView
    private var videoUri: Uri? = null

    // متغيرات لحفظ مكان التوقف
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
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUri!!)
            exoPlayer.setMediaItem(mediaItem)

            // استخدام متغيرات مباشرة بدل reassignment لـ val
            exoPlayer.seekForwardIncrement = 10000L // 10 ثواني
            exoPlayer.seekBackIncrement = 10000L // 10 ثواني

            exoPlayer.prepare()

            // تفعيل الإيماءات من DoubleTapPlayerView
            playerView.setPlayer(exoPlayer)

            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            savePlayerState(exoPlayer)
            exoPlayer.release()
        }
        player = null
    }

    private fun savePlayerState(exoPlayer: Player) {
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
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (player == null) initializePlayer()
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
