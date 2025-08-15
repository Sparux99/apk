package com.amine.player 

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var videoAdapter: VideoAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadVideos()
            } else {
                // يمكنك إظهار رسالة للمستخدم هنا تشرح لماذا تحتاج الإذن
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // التحقق من توافق اسم الحزمة
        val expectedPackageName = "com.amine.player"
        val actualPackageName = packageName
        if (actualPackageName != expectedPackageName) {
            throw IllegalStateException("Package name mismatch: expected $expectedPackageName but got $actualPackageName")
        }
        
        Log.d("AppConsistencyCheck", "MainActivity started successfully! Package name is consistent.")

        videosRecyclerView = findViewById(R.id.videos_recycler_view)
        loadingIndicator = findViewById(R.id.loading_indicator)
        videosRecyclerView.layoutManager = LinearLayoutManager(this)

        checkPermissionAndLoadVideos()


        videosRecyclerView = findViewById(R.id.videos_recycler_view)
        loadingIndicator = findViewById(R.id.loading_indicator)
        videosRecyclerView.layoutManager = LinearLayoutManager(this)

        checkPermissionAndLoadVideos()
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
                    // الكود الذي سينفذ عند الضغط على فيديو
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