package com.amine.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    // overlay controls
    private lateinit var seekBar: SeekBar
    private lateinit var tvOverlay: TextView
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnLock: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnSkipBack: ImageButton
    private lateinit var btnSkipForward: ImageButton
    private lateinit var btnSpeed: ImageButton
    private lateinit var overlay: View
    private lateinit var topBar: View

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
    private val autoHideDelay = 3000L
    private val textHideDelay = 1400L

    private val hideControlsRunnable = Runnable { overlay.visibility = View.GONE }
    private val hideOverlayTextRunnable = Runnable { tvOverlay.visibility = View.GONE }

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
        setContentView(R.layout.activity_player) // ملف XML الذي سترفعه/تستبدله

        playerView = findViewById(R.id.player_view)
        // تأكد تعطيل الكنترول الافتراضي
        playerView.useController = false

        overlay = findViewById(R.id.overlay)
        topBar = findViewById(R.id.top_bar)
        seekBar = findViewById(R.id.seekBar)
        tvOverlay = findViewById(R.id.tv_overlay)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnLock = findViewById(R.id.btn_lock)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnSkipBack = findViewById(R.id.btn_skip_back)
        btnSkipForward = findViewById(R.id.btn_skip_forward)
        btnSpeed = findViewById(R.id.btn_speed)

        videoUriString = intent?.dataString

        // UI actions
        btnFullscreen.setOnClickListener { toggleFullscreen(); showControls() }
        btnLock.setOnClickListener { toggleLock(); showControls() }
        btnPlayPause.setOnClickListener { togglePlayPause(); showControls() }
        btnSkipBack.setOnClickListener { skipSeconds(-10_000L); showControls() }
        btnSkipForward.setOnClickListener { skipSeconds(+10_000L); showControls() }
        btnSpeed.setOnClickListener { showSpeedMenu(it as View); showControls() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player?.seekTo(progress.toLong() * 1000L)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacks(hideControlsRunnable) }
            override fun onStopTrackingTouch(sb: SeekBar?) { handler.postDelayed(hideControlsRunnable, autoHideDelay) }
        })

        // tap overlay to toggle controls
        playerView.setOnClickListener { if (!isLocked) toggleControlsVisibility() }

        // gesture handling (volume/brightness/seek)
        playerView.setOnTouchListener { v, event ->
            if (isLocked) return@setOnTouchListener true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x; startY = event.y
                    isDragging = true; gestureMode = GestureMode.NONE
                    initialVolume = getCurrentVolume()
                    initialBrightness = window.attributes.screenBrightness
                    if (initialBrightness < 0f) initialBrightness = 0.5f
                    initialPosition = player?.currentPosition ?: 0L
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) return@setOnTouchListener true
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (gestureMode == GestureMode.NONE) {
                        if (abs(dx) > abs(dy)) gestureMode = GestureMode.SEEK
                        else {
                            val half = v.width / 2
                            gestureMode = if (startX < half) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                        }
                    }
                    when (gestureMode) {
                        GestureMode.VOLUME -> {
                            handleVolumeGesture(dy)
                            showOverlayText("Volume: ${getCurrentVolume()}%")
                        }
                        GestureMode.BRIGHTNESS -> {
                            handleBrightnessGesture(dy)
                            showOverlayText("Brightness: ${(window.attributes.screenBrightness * 100).toInt()}%")
                        }
                        GestureMode.SEEK -> {
                            handleSeekGesture(dx)
                            showOverlayText("Seek: ${ (player?.currentPosition ?: 0L) / 1000 }s")
                        }
                        else -> {}
                    }
                    showControls() // keep controls visible while interacting
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    gestureMode = GestureMode.NONE
                }
            }
            true
        }
    }

    // controls visibility helpers
    private fun showControls() {
        handler.removeCallbacks(hideControlsRunnable)
        overlay.visibility = View.VISIBLE
        // animate if desired
        handler.postDelayed(hideControlsRunnable, autoHideDelay)
    }
    private fun hideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        overlay.visibility = View.GONE
    }
    private fun toggleControlsVisibility() {
        if (overlay.visibility == View.VISIBLE) hideControls() else showControls()
    }

    private fun showOverlayText(text: String) {
        tvOverlay.text = text
        tvOverlay.visibility = View.VISIBLE
        handler.removeCallbacks(hideOverlayTextRunnable)
        handler.postDelayed(hideOverlayTextRunnable, textHideDelay)
    }

    // playback controls
    private fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
            updatePlayIcon()
        }
    }
    private fun updatePlayIcon() {
        val playing = player?.isPlaying == true
        btnPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }
    private fun skipSeconds(deltaMs: Long) {
        player?.let { p ->
            val target = min(max(0L, p.currentPosition + deltaMs), if (p.duration > 0) p.duration else Long.MAX_VALUE)
            p.seekTo(target)
        }
    }

    private fun showSpeedMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val speeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
        speeds.forEachIndexed { i, s -> popup.menu.add(0, i, i, "${s}x") }
        popup.setOnMenuItemClickListener { item ->
            val speed = speeds[item.itemId]
            player?.setPlaybackParameters(PlaybackParameters(speed))
            showOverlayText("Speed: ${speed}x")
            true
        }
        popup.show()
    }

    // existing gesture handlers (volume/brightness/seek) reused
    private fun handleVolumeGesture(dy: Float) {
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH)
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        var newVol = (initialVolume + (delta * max)).toInt()
        if (newVol < 0) newVol = 0
        if (newVol > max) newVol = max
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
    }

    private fun handleBrightnessGesture(dy: Float) {
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH)
        var newBrightness = initialBrightness + delta
        newBrightness = newBrightness.coerceIn(0f, 1f)
        val lp = window.attributes
        lp.screenBrightness = newBrightness
        window.attributes = lp
    }

    private fun handleSeekGesture(dx: Float) {
        val w = resources.displayMetrics.widthPixels
        val duration = player?.duration ?: 0L
        if (duration <= 0) return
        val deltaMs = ((dx / w) * (duration * 0.3)).toLong()
        var target = initialPosition + deltaMs
        target = target.coerceIn(0L, duration)
        player?.seekTo(target)
    }

    private fun getCurrentVolumePercent(): Int {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val cur = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        return (cur * 100) / max
    }
    private fun getCurrentVolume(): Int = getCurrentVolumePercent()

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
        val resName = if (isLocked) "ic_lock_closed" else "ic_lock_open"
        val resId = resources.getIdentifier(resName, "drawable", packageName)
        if (resId != 0) btnLock.setImageResource(resId) else btnLock.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        btnFullscreen.isVisible = !isLocked
        seekBar.isEnabled = !isLocked
    }

    // initialize player (nullable)
    private fun initializePlayer(uriString: String?) {
        val uriToPlay = uriString ?: return
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.fromUri(uriToPlay)
            exo.setMediaItem(item)
            exo.playWhenReady = true
            exo.prepare()
        }
        updatePlayIcon()
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
