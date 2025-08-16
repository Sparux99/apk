package com.amine.player
package com.amine.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: com.github.dimezis.gestureviews.GesturePlayerView
    private lateinit var playerView: PlayerView
    private var videoUri: Uri? = null

    // ▼▼▼ متغيرات جديدة لحفظ مكان التوقف ▼▼▼
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

        // استعادة مكان التوقف المحفوظ
        restorePlayerState()

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUri!!)
            exoPlayer.setMediaItem(mediaItem)

            exoPlayer.setSeekForwardIncrement(seekTime.toLong())
            exoPlayer.setSeekBackIncrement(seekTime.toLong())
            
            exoPlayer.prepare()
            
            // ▼▼▼ تفعيل الإيماءات للمكتبة ▼▼▼
            playerView.setPlayer(exoPlayer)

            // ▼▼▼ الانتقال إلى آخر مكان توقف فيه المستخدم ▼▼▼
            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            // ▼▼▼ حفظ مكان التوقف قبل تحرير المشغل ▼▼▼
            savePlayerState(exoPlayer)
            exoPlayer.release()
        }
        player = null
    }

    // ▼▼▼ دالة لحفظ الحالة ▼▼▼
    private fun savePlayerState(exoPlayer: Player) {
        playbackPosition = exoPlayer.currentPosition
        currentItem = exoPlayer.currentMediaItemIndex
        playWhenReady = exoPlayer.playWhenReady

        val prefs = getSharedPreferences("PlayerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("playbackPosition_${videoUri.toString()}", playbackPosition).apply()
    }

    // ▼▼▼ دالة لاستعادة الحالة ▼▼▼
    private fun restorePlayerState() {
        val prefs = getSharedPreferences("PlayerPrefs", Context.MODE_PRIVATE)
        playbackPosition = prefs.getLong("playbackPosition_${videoUri.toString()}", 0L)
    }
    
    // ... (بقية دوال onStart, onResume, onPause, onStop ودالة hideSystemUI لم تتغير)
    // ...

    private fun hideSystemUI() {
        // تفعيل وضع ملء الشاشة الكامل
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // دورة حياة الـ Activity لإدارة المشغل بكفاءة
    public override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}