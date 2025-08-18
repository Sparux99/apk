package com.amine.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PlayerActivity : AppCompatActivity() {

    // ---------- Views & Player ----------
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    // overlay controls
    private lateinit var overlay: RelativeLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var topBar: LinearLayout
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
    private lateinit var brightnessBar: ProgressBar
    private lateinit var volumeBar: ProgressBar

    // ---------- Playback source ----------
    private var videoUriString: String? = null
    private var initialPosition = 0L

    // ---------- Gesture state ----------
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var gestureMode: GestureMode = GestureMode.NONE
    private var initialVolume = 0
    private var initialBrightness = 0f
    private var initialSeekPosition = 0L
    private val gestureTolerance = 20f

    // ---------- Lock state (Finite State Machine) ----------
    private enum class LockState { UNLOCKED, LOCKED_VISIBLE, LOCKED_HIDDEN }
    private var lockState: LockState = LockState.UNLOCKED

    // ---------- Fullscreen state ----------
    private var isFullscreen = false

    // ---------- Handler & delays ----------
    private val handler = Handler(Looper.getMainLooper())
    private val autoHideDelay = 3000L      // إخفاء عناصر التحكم تلقائياً
    private val textHideDelay = 1400L      // إخفاء نصّ الـ overlay
    private val gestureUIHideDelay = 500L  // إخفاء شريط الصوت/السطوع بعد التمرير
    private val lockButtonHideDelay = 1000L// إخفاء زر القفل بعد ظهوره مؤقتًا

    // إخفاءات
    private val hideControlsRunnable = Runnable { 
        if (lockState == LockState.UNLOCKED) {
            overlay.visibility = View.GONE
        } else if (lockState == LockState.LOCKED_VISIBLE) {
            // نخفي البارات فقط، ونترك overlay ظاهر عشان يظل زر القفل
            bottomBar.isVisible = false
            topBar.isVisible = false
        }
    }

    private val hideOverlayTextRunnable = Runnable { tvOverlay.visibility = View.GONE }
    private val hideGestureUIRunnable = Runnable {
        brightnessBar.visibility = View.GONE
        volumeBar.visibility = View.GONE
    }
    // مهم: عند انتهاء المهلة، اخف الزر وحوّل الحالة إلى HIDDEN
    private val hideLockButtonRunnable = Runnable {
        if (lockState == LockState.LOCKED_VISIBLE) {
            lockState = LockState.LOCKED_HIDDEN
            applyState()
        }
    }

    // تحديث التقدّم الدوري
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

    private enum class GestureMode { NONE, VOLUME, BRIGHTNESS, SEEK, TAP }

    // ------------------ Lifecycle ------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeHelper.getSavedTheme(this))  
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // جمع الـ Views
        playerView = findViewById(R.id.player_view)
        playerView.useController = false
        overlay = findViewById(R.id.overlay)
        bottomBar = findViewById(R.id.bottom_bar)
        topBar = findViewById(R.id.top_bar)
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

        // استعادة حالة المشغل من الـ savedInstanceState أو SharedPreferences
        savedInstanceState?.let {
            lockState = LockState.valueOf(it.getString("lockState", LockState.UNLOCKED.name))
            initialPosition = it.getLong("position", 0L)
        } ?: run {
            val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val rememberPositionEnabled = prefs.getBoolean("RememberPosition", true)
            if (rememberPositionEnabled) {
                val videoId = intent.dataString?.hashCode() ?: 0
                initialPosition = prefs.getLong("lastPosition_$videoId", 0L)
            } else {
                initialPosition = 0L
            }
        }

        // ---------- أزرار التحكم ----------
        btnFullscreen.setOnClickListener { toggleFullscreen(); showControls() }
        btnLock.setOnClickListener {
            when (lockState) {
                LockState.UNLOCKED -> enterLock()
                LockState.LOCKED_VISIBLE, LockState.LOCKED_HIDDEN -> exitLock()
            }
        }
        
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val skipMs = prefs.getInt("SkipDuration", 10) * 1000L

        btnPlayPause.setOnClickListener { if (player?.playbackState == Player.STATE_ENDED) {
                                            player?.seekTo(0);
                                            player?.play();
                                        } else {
                                                togglePlayPause();
                                                }; showControls(); }
        btnSkipBack.setOnClickListener {  skipSeconds(-skipMs); showControls() }
        btnSkipForward.setOnClickListener {  skipSeconds(+skipMs); showControls() }
        btnSpeed.setOnClickListener { showSpeedMenu(it as View); showControls() }

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player?.seekTo(progress.toLong() * 1000L)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacks(hideControlsRunnable) }
            override fun onStopTrackingTouch(sb: SeekBar?) { handler.postDelayed(hideControlsRunnable, autoHideDelay) }
        })

        // ---------- لمسات / إيماءات على PlayerView ----------
        playerView.setOnTouchListener { v, event ->
            // إذا الشاشة مقفلة، امتصّ اللمسات واظهر زر القفل مؤقتًا
            if (lockState != LockState.UNLOCKED) {
                if (event.action == MotionEvent.ACTION_UP) {
                    lockState = LockState.LOCKED_VISIBLE
                    applyState()                // يجعل overlay مرئيًا مع إخفاء الأشرطة
                    btnLock.isVisible = true
                    handler.removeCallbacks(hideLockButtonRunnable)
                    handler.postDelayed(hideLockButtonRunnable, lockButtonHideDelay)
                }
                return@setOnTouchListener true
            }

            // إذا غير مقفل، نفذ منطق الإيماءات الطبيعي
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isDragging = true
                    gestureMode = GestureMode.NONE
                    initialVolume = getCurrentVolume()
                    initialBrightness = getInitialBrightness()
                    initialSeekPosition = player?.currentPosition ?: 0L
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) return@setOnTouchListener true
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = sqrt(dx * dx + dy * dy)

                    if (gestureMode == GestureMode.NONE) {
                        if (distance > gestureTolerance) {
                            if (abs(dx) > abs(dy)) gestureMode = GestureMode.SEEK
                            else {
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
                    val distance = sqrt(dx * dx + dy * dy)
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

        applyState()
        if (lockState == LockState.LOCKED_VISIBLE) {
            handler.postDelayed(hideLockButtonRunnable, lockButtonHideDelay)
        }
    }

    // ------------------ Helper functions ------------------
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun applyState() {
        when (lockState) {
            LockState.UNLOCKED -> {
                overlay.visibility = View.VISIBLE
                bottomBar.isVisible = true
                topBar.isVisible = true
                seekBar.isEnabled = true
                btnLock.setImageResource(R.drawable.ic_lock_open)
                btnLock.isVisible = true
                setGesturesEnabled(true)
                enableImmersive(false)
            }
            LockState.LOCKED_VISIBLE -> {
                // مهم: اترك overlay مرئيًا حتى تظهر أيقونة القفل
                overlay.visibility = View.VISIBLE
                bottomBar.isVisible = false
                topBar.isVisible = false
                seekBar.isEnabled = false
                btnLock.setImageResource(R.drawable.ic_lock_closed)
                btnLock.isVisible = true
                setGesturesEnabled(false)
                enableImmersive(true)
            }
            LockState.LOCKED_HIDDEN -> {
                // في الوضع المخفي، overlay مخفي بالكامل
                overlay.visibility = View.GONE
                bottomBar.isVisible = false
                topBar.isVisible = false
                seekBar.isEnabled = false
                btnLock.setImageResource(R.drawable.ic_lock_closed)
                btnLock.isVisible = false
                setGesturesEnabled(false)
                enableImmersive(true)
            }
        }
    }

    private fun showControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(hideLockButtonRunnable)
        overlay.visibility = View.VISIBLE
        handler.postDelayed(hideControlsRunnable, autoHideDelay)
    }

    private fun hideControls() {
    handler.removeCallbacks(hideControlsRunnable)
    if (lockState == LockState.UNLOCKED) {
        overlay.visibility = View.GONE
    } else if (lockState == LockState.LOCKED_VISIBLE) {
        bottomBar.isVisible = false
        topBar.isVisible = false
        // نخلي overlay ظاهر حتى يظل زر القفل
        overlay.visibility = View.VISIBLE
        btnLock.isVisible = true
    }
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

    // ------------------ Playback controls ------------------
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
            val dur = if (p.duration > 0) p.duration else Long.MAX_VALUE
            val target = min(max(0L, p.currentPosition + deltaMs), dur)
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

    // ------------------ Gesture handlers ------------------
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
        // تحسين الحساسية: اختر الأكبر بين 25% من المدة و 20 ثانية
        val maxSeekMs = max((duration * 0.25f).toLong(), 20_000L)
        val deltaMs = ((dx / w) * maxSeekMs).toLong()
        val target = (initialSeekPosition + deltaMs).coerceIn(0L, duration)
        player?.seekTo(target)
    }

    private fun getCurrentVolume(): Int {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
    }

    private fun getInitialBrightness(): Float {
        val attrValue = window.attributes.screenBrightness
        if (attrValue >= 0f) return attrValue
        // attrValue == -1 => اتبع سطوع النظام: نقرأه و نحوله إلى مدى 0..1
        return try {
            val sys = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) // 0..255 (أحيانًا أعلى)
            val maxSys = 255f
            (sys / maxSys).coerceIn(0f, 1f)
        } catch (_: Exception) {
            0.5f // قيمة احتياطية
        }
    }

    // ------------------ Fullscreen & Immersive ------------------
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        val flags = if (isFullscreen) {
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        } else {
            0
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (flags != 0) {
            window.addFlags(flags)
        }
        btnFullscreen.setImageResource(if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen)
    }

    private fun enableImmersive(enable: Boolean) {
        val window = window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.let {
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enable) it.hide(android.view.WindowInsets.Type.systemBars())
            else it.show(android.view.WindowInsets.Type.systemBars())
        }
    }

    // ------------------ Lock state transitions ------------------
    private fun enterLock() {
        lockState = LockState.LOCKED_VISIBLE
        applyState()
        handler.removeCallbacks(hideLockButtonRunnable)
        handler.postDelayed(hideLockButtonRunnable, lockButtonHideDelay)
    }

    private fun exitLock() {
        lockState = LockState.UNLOCKED
        handler.removeCallbacks(hideLockButtonRunnable)
        applyState()
        showControls()
    }

    private fun setGesturesEnabled(enabled: Boolean) {
        // لضمان استقبال اللمسات حتى في وضع القفل، اجعل clickable دائمًا true
        playerView.isClickable = true
        playerView.isFocusable = enabled
    }

    // ------------------ Player init / release ------------------
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
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(hideOverlayTextRunnable)
        handler.removeCallbacks(hideGestureUIRunnable)
        handler.removeCallbacks(hideLockButtonRunnable)
        handler.removeCallbacks(updateProgressRunnable)
    }

    override fun onStop() {
        super.onStop()
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val rememberPositionEnabled = prefs.getBoolean("RememberPosition", true)
        if (rememberPositionEnabled) {
            val videoId = intent.dataString?.hashCode() ?: 0
            val editor = prefs.edit()
            editor.putLong("lastPosition_$videoId", player?.currentPosition ?: 0L)
            editor.apply()
        }
        initialPosition = player?.currentPosition ?: initialPosition
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("lockState", lockState.name)
        outState.putLong("position", player?.currentPosition ?: initialPosition)
    }

    private fun initializePlayer(uriString: String?) {
        val uriToPlay = uriString ?: return
        if (player != null) return

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.fromUri(uriToPlay)

            // النقطة الحاسمة لمنع البدء من البداية:
            exo.setMediaItem(item, initialPosition)
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    updatePlayIcon()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // إعادة تعيين الموضع
                        initialPosition = 0L
                        player?.seekTo(0)
                        // إظهار زر إعادة التشغيل بدل التشغيل/الإيقاف
                        btnPlayPause.setImageResource(R.drawable.ic_replay)
                        // إلغاء إبقاء الشاشة مشتعلة
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else if (playbackState == Player.STATE_READY) {
                        updatePlayIcon()
                    }
}

            })
        }
        handler.removeCallbacks(updateProgressRunnable)
        handler.post(updateProgressRunnable)
        applyState()
        showControls()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(hideOverlayTextRunnable)
        handler.removeCallbacks(hideGestureUIRunnable)
        handler.removeCallbacks(hideLockButtonRunnable)
        playerView.player = null
        player?.release()
        player = null
    }
}