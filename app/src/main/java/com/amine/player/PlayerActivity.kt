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
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerActivity : AppCompatActivity() {

    // مشغل الفيديو ExoPlayer
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    // عناصر التحكم المخصصة (overlay controls)
    private lateinit var seekBar: SeekBar
    private lateinit var tvPosition: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvOverlay: TextView
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnLock: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnSkipBack: ImageButton
    private lateinit var btnSkipForward: ImageButton
    private lateinit var btnSpeed: ImageButton
    private lateinit var overlay: View
    private lateinit var brightnessBar: ProgressBar
    private lateinit var volumeBar: ProgressBar

    private var videoUriString: String? = null

    // حالة الإيماءات
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var gestureMode: GestureMode = GestureMode.NONE
    private var initialVolume = 0
    private var initialBrightness = 0f
    private var initialPosition = 0L
    private val gestureTolerance = 20f

    // حالة الشاشة والقفل
    private var isLocked = false
    private var isFullscreen = false

    // معالج الرسائل لتأخير الإجراءات
    private val handler = Handler(Looper.getMainLooper())
    private val autoHideDelay = 3000L
    private val textHideDelay = 1400L
    private val gestureUIHideDelay = 500L

    private val hideControlsRunnable = Runnable { overlay.visibility = View.GONE }
    private val hideOverlayTextRunnable = Runnable { tvOverlay.visibility = View.GONE }
    private val hideGestureUIRunnable = Runnable {
        brightnessBar.visibility = View.GONE
        volumeBar.visibility = View.GONE
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && p.duration > 0) {
                val pos = p.currentPosition
                val dur = p.duration
                seekBar.max = (dur / 1000).toInt()
                seekBar.progress = (pos / 1000).toInt()
                tvPosition.text = formatTime(pos)
                tvDuration.text = formatTime(dur)
            }
            handler.postDelayed(this, 500)
        }
    }

    enum class GestureMode { NONE, VOLUME, BRIGHTNESS, SEEK, TAP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // ربط عناصر الواجهة بمتغيراتها
        playerView = findViewById(R.id.player_view)
        playerView.useController = false
        overlay = findViewById(R.id.overlay)
        seekBar = findViewById(R.id.seekBar)
        tvPosition = findViewById(R.id.tv_position)
        tvDuration = findViewById(R.id.tv_duration)
        tvOverlay = findViewById(R.id.tv_overlay)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnLock = findViewById(R.id.btn_lock)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnSkipBack = findViewById(R.id.btn_skip_back)
        btnSkipForward = findViewById(R.id.btn_skip_forward)
        btnSpeed = findViewById(R.id.btn_speed)
        brightnessBar = findViewById(R.id.brightness_bar)
        volumeBar = findViewById(R.id.volume_bar)

        videoUriString = intent?.dataString

        // معالجة ضغط الأزرار
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

        // معالجة اللمسات والإيماءات
        playerView.setOnTouchListener { v, event ->
            // منطق خاص عند قفل الشاشة
            if (isLocked) {
                if (event.action == MotionEvent.ACTION_UP) {
                    // عند النقر، فقط أظهر زر القفل ليتمكن المستخدم من إلغاء القفل
                    btnLock.isVisible = true
                    // لا تتركه يختفي بعد فترة
                    handler.removeCallbacks(hideControlsRunnable)
                }
                return@setOnTouchListener true
            }

            // منطق الإيماءات الطبيعي
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isDragging = true
                    gestureMode = GestureMode.NONE
                    initialVolume = getCurrentVolume()
                    initialBrightness = window.attributes.screenBrightness.let { if (it < 0f) 0.5f else it }
                    initialPosition = player?.currentPosition ?: 0L
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) return@setOnTouchListener true
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (gestureMode == GestureMode.NONE) {
                        if (distance > gestureTolerance) {
                            if (abs(dx) > abs(dy)) {
                                gestureMode = GestureMode.SEEK
                            } else {
                                val half = v.width / 2
                                gestureMode = if (startX < half) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                            }
                        } else {
                            return@setOnTouchListener false
                        }
                    }

                    when (gestureMode) {
                        GestureMode.VOLUME -> {
                            handleVolumeGesture(dy)
                            volumeBar.visibility = View.VISIBLE
                        }
                        GestureMode.BRIGHTNESS -> {
                            handleBrightnessGesture(dy)
                            brightnessBar.visibility = View.VISIBLE
                        }
                        GestureMode.SEEK -> {
                            handleSeekGesture(dx)
                            showOverlayText("التقدم: ${formatTime(player?.currentPosition ?: 0L)}")
                        }
                        else -> {}
                    }
                    showControls()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < gestureTolerance) {
                        toggleControlsVisibility()
                    }
                    isDragging = false
                    gestureMode = GestureMode.NONE
                    handler.postDelayed(hideGestureUIRunnable, gestureUIHideDelay)
                }
            }
            true
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun showControls() {
        handler.removeCallbacks(hideControlsRunnable)
        overlay.visibility = View.VISIBLE
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

    // معالجات الإيماءات
    private fun handleVolumeGesture(dy: Float) {
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH) * 2.5f
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val newVol = (initialVolume + (delta * max)).toInt().coerceIn(0, max)
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
        volumeBar.max = max
        volumeBar.progress = newVol
        showOverlayText("الصوت: ${newVol * 100 / max}%")
    }

    private fun handleBrightnessGesture(dy: Float) {
        val screenH = resources.displayMetrics.heightPixels
        val delta = (-dy / screenH) * 2.5f
        val newBrightness = (initialBrightness + delta).coerceIn(0f, 1f)
        val lp = window.attributes
        lp.screenBrightness = newBrightness
        window.attributes = lp
        brightnessBar.max = 100
        brightnessBar.progress = (newBrightness * 100).toInt()
        showOverlayText("الإضاءة: ${(newBrightness * 100).toInt()}%")
    }

    private fun handleSeekGesture(dx: Float) {
        val w = resources.displayMetrics.widthPixels
        val duration = player?.duration ?: 0L
        if (duration <= 0) return
        val deltaMs = ((dx / w) * (duration * 0.15)).toLong()
        val target = (initialPosition + deltaMs).coerceIn(0L, duration)
        player?.seekTo(target)
    }

    private fun getCurrentVolume(): Int {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val cur = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        return cur
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
        val resName = if (isLocked) "ic_lock_closed" else "ic_lock_open"
        val resId = resources.getIdentifier(resName, "drawable", packageName)
        btnLock.setImageResource(if (resId != 0) resId else android.R.drawable.ic_menu_close_clear_cancel)

        if (isLocked) {
            // عند القفل، أخفِ كل شيء ما عدا زر القفل نفسه
            overlay.visibility = View.GONE
            btnLock.isVisible = true
            btnFullscreen.isVisible = false
            seekBar.isEnabled = false
        } else {
            // عند إلغاء القفل، أعد كل شيء إلى طبيعته
            overlay.visibility = View.VISIBLE
            btnFullscreen.isVisible = true
            seekBar.isEnabled = true
            showControls()
        }
    }

    private fun initializePlayer(uriString: String?) {
        val uriToPlay = uriString ?: return
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.fromUri(uriToPlay)
            exo.setMediaItem(item)
            exo.playWhenReady = true
            exo.prepare()

            // إضافة مُستمع (Listener) لتعديل حالة الشاشة
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                // تحديث أيقونة التشغيل بمجرد جاهزية المشغل للتشغيل
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && exo.playWhenReady) {
                        updatePlayIcon()
                    }
                }
            })
        }
        updatePlayIcon()
        handler.post(updateProgressRunnable)
        showControls()
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
        if (player != null) {
            handler.post(updateProgressRunnable)
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