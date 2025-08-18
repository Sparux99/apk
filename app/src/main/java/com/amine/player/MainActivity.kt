package com.amine.player

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import com.amine.player.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var toggle: ActionBarDrawerToggle
    private val fullVideoList = mutableListOf<Video>()

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recreate()
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) loadVideos() else
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        setTheme(prefs.getInt("AppTheme", R.style.Theme_Amine))

        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.videosRecyclerView.layoutManager = LinearLayoutManager(this)

        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.navView.setNavigationItemSelectedListener(this)

        checkPermissionAndLoadVideos()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterVideos(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        val currentList = videoAdapter.videos.toMutableList()
        when (item.itemId) {
            R.id.sort_by_date_desc -> currentList.sortByDescending { it.dateAdded }
            R.id.sort_by_date_asc -> currentList.sortBy { it.dateAdded }
            R.id.sort_by_name_asc -> currentList.sortBy { it.title.lowercase() }
            R.id.sort_by_name_desc -> currentList.sortByDescending { it.title.lowercase() }
            else -> return super.onOptionsItemSelected(item)
        }
        videoAdapter.updateVideos(currentList)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dark_mode -> {
                val current = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                AppCompatDelegate.setDefaultNightMode(
                    if (current == Configuration.UI_MODE_NIGHT_YES)
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                )
            }
            R.id.nav_settings -> settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerLayout.closeDrawers()
        return true
    }

    private fun filterVideos(query: String?) {
        val filtered = if (query.isNullOrBlank()) fullVideoList
        else fullVideoList.filter { it.title.contains(query, true) }
        videoAdapter.updateVideos(filtered)
    }

    private fun checkPermissionAndLoadVideos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            loadVideos()
        else requestPermissionLauncher.launch(permission)
    }

    private fun loadVideos() {
        binding.loadingIndicator.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val videoList = fetchVideosFromDevice()
            fullVideoList.clear()
            fullVideoList.addAll(videoList)
            withContext(Dispatchers.Main) {
                binding.loadingIndicator.visibility = View.GONE
                videoAdapter = VideoAdapter(fullVideoList) { video ->
                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        data = video.contentUri
                    })
                }
                binding.videosRecyclerView.adapter = videoAdapter
            }
        }
    }

    private fun fetchVideosFromDevice(): List<Video> {
        val videoList = mutableListOf<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)
                val duration = cursor.getLong(durCol)
                val dateAdded = cursor.getLong(dateCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                videoList.add(Video(id, title, duration, contentUri, dateAdded))
            }
        }
        return videoList
    }
}
