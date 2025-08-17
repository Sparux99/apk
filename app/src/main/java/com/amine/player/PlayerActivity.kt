package com.amine.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var tvOverlay: TextView
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnLock: ImageButton

    private var videoUriString: String? = null

    // gesture state
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var gestureMode: GestureMode = GestureMode.NONE
    private var initialVolume = 0
    private var initialBrightness = 0f
    private var initialPosition = 0L

    private var isLocked = false
    private var isFullscreen = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && p.duration > 0) {
                val pos = p.currentPosition
                val dur = p.duration
                seekBar.max = (dur / 1000).toInt()
                seekBar.progress = (pos / 1000).toInt()
            }
            handler.postDelayed(this, 500)
        }
    }

    enum class GestureMode { NONE, VOLUME, BRIGHTNESS, SEEK }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_custom)

        playerView = findViewById(R.id.player_view)
        seekBar = findViewById(R.id.seekBar)
        tvOverlay = findViewById(R.id.tv_overlay)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnLock = findViewById(R.id.btn_lock)

        videoUriString = intent?.dataString

        // UI actions
        btnFullscreen.setOnClickListener { toggleFullscreen() }
        btnLock.setOnClickListener { toggleLock() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong() * 1000L)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Gesture handling on player view
        playerView.setOnTouchListener { v, event ->
            if (isLocked) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isDragging = true
                    gestureMode = GestureMode.NONE

                    // save initial values
                    initialVolume = getCurrentVolume()
                    initialBrightness = window.attributes.screenBrightness
                    if (initialBrightness < 0f) initialBrightness = 0.5f
                    initialPosition = player?.currentPosition ?: 0L
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) return@setOnTouchListener true
                    val dx = event.x - startX
                    val dy = event.y - startY

                    // determine mode if not set
                    if (gestureMode == GestureMode.NONE) {
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            gestureMode = GestureMode.SEEK
                        } else {
                            // vertical: left half -> brightness, right half -> volume
                            val half = v.width / 2
                            gestureMode = if (startX < half) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                        }
                    }

                    when (gestureMode) {
                        GestureMode.VOLUME -> handleVolumeGesture(dy)
                        GestureMode.BRIGHTNESS -> handleBrightnessGesture(dy)
                        GestureMode.SEEK -> handleSeekGesture(dx)
                        else -> {}
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // finalize seek if any
                    if (gestureMode == GestureMode.SEEK) {
                        // nothing extra because we updated seek on move; ensure final
                        val finalPos = player?.currentPosition ?: initialPosition
                        tvOverlay.text = "" // hide
                        tvOverlay.isVisible = false
                    }
                    isDragging = false
                    gestureMode = GestureMode.NONE
                }
            }
            true
        }
    }

    private fun handleVolumeGesture(dy: Float) {
        // dy positive -> moving down -> decrease volume
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH) // normalized -1..1
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        var newVol = (initialVolume + (delta * max)).toInt()
        if (newVol < 0) newVol = 0
        if (newVol > max) newVol = max
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
        val percent = (newVol * 100) / max
        tvOverlay.text = "Volume: $percent%"
        tvOverlay.isVisible = true
    }

    private fun handleBrightnessGesture(dy: Float) {
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH) // normalized
        var newBrightness = initialBrightness + delta
        if (newBrightness < 0f) newBrightness = 0f
        if (newBrightness > 1f) newBrightness = 1f
        val lp = window.attributes
        lp.screenBrightness = newBrightness
        window.attributes = lp
        val percent = (newBrightness * 100).toInt()
        tvOverlay.text = "Brightness: $percent%"
        tvOverlay.isVisible = true
    }

    private fun handleSeekGesture(dx: Float) {
        val w = resources.displayMetrics.widthPixels
        val duration = player?.duration ?: 0L
        if (duration <= 0) return
        // map dx to +/- 30% of duration
        val deltaMs = ((dx / w) * (duration * 0.3)).toLong()
        var target = initialPosition + deltaMs
        if (target < 0) target = 0
        if (target > duration) target = duration
        player?.seekTo(target)
        val s = target / 1000
        tvOverlay.text = "Seek: ${s}s"
        tvOverlay.isVisible = true
    }

    private fun getCurrentVolume(): Int {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun toggleLock() {
        isLocked = !isLocked
        btnLock.setImageResource(if (isLocked) android.R.drawable.ic_lock_lock else android.R.drawable.ic_lock_open)
        // hide controls when locked
        btnFullscreen.isVisible = !isLocked
        btnLock.isVisible = true
        seekBar.isEnabled = !isLocked
    }

    private fun initializePlayer(uriString: String) {
        val uriToPlay = uriString ?: return
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.fromUri(uriToPlay)
            exo.setMediaItem(item)
            exo.playWhenReady = true
            exo.prepare()
        }
        handler.post(updateProgressRunnable)
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updateProgressRunnable)
        player?.release()
        player = null
    }

    override fun onStart() {
        super.onStart()
        initializePlayer(videoUriString)
    }

    override fun onResume() {
        super.onResume()
        // resume progress updates
        handler.post(updateProgressRunnable)
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