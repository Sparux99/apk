package com.amine.player

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. جعل الـ Activity "تستمع" لنقرات القائمة
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var videoAdapter: VideoAdapter

    // Navigation Drawer components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadVideos()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val themeId = prefs.getInt("AppTheme", R.style.Theme_Amine)
        setTheme(themeId)

        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // RecyclerView + ProgressBar
        videosRecyclerView = findViewById(R.id.videos_recycler_view)
        loadingIndicator = findViewById(R.id.loading_indicator)
        videosRecyclerView.layoutManager = LinearLayoutManager(this)

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view) // تأكد من أن الـ ID هو nav_view كما في ملف الـ XML
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 2. تعيين المستمع ليكون هذه الـ Activity نفسها
        navigationView.setNavigationItemSelectedListener(this)

        checkPermissionAndLoadVideos()
    }

    // هذه الدالة ضرورية لعمل زر "الهمبرغر" في شريط العنوان
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // 3. الدالة الجديدة التي تتعامل مع جميع نقرات عناصر القائمة
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // تأكد من أن هذا الـ ID يطابق ما وضعته في ملف nav_menu.xml
            R.id.nav_dark_mode -> {
                // الكود الخاص بتبديل الوضع المظلم
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                // إعادة إنشاء الواجهة لتطبيق التغيير فوراً
                recreate()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            // أضف أي عناصر أخرى هنا
        }
        // إغلاق القائمة بعد الاختيار
        drawerLayout.closeDrawers()
        return true
    }

    private fun checkPermissionAndLoadVideos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                loadVideos()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadVideos() {
        loadingIndicator.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            val videoList = fetchVideosFromDevice()
            withContext(Dispatchers.Main) {
                loadingIndicator.visibility = View.GONE
                videoAdapter = VideoAdapter(videoList) { video ->
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        data = video.contentUri
                    }
                    startActivity(intent)
                }
                videosRecyclerView.adapter = videoAdapter
            }
        }
    }

    private fun fetchVideosFromDevice(): List<Video> {
        val videoList = mutableListOf<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                videoList.add(Video(id, title, duration, contentUri))
            }
        }
        return videoList
    }
}