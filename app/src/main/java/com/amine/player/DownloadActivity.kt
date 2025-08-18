package com.amine.player

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "تحميل فيديو"

        val urlEditText = findViewById<EditText>(R.id.url_edit_text)
        val downloadButton = findViewById<Button>(R.id.download_button)

        downloadButton.setOnClickListener {
            val url = urlEditText.text.toString()
            if (URLUtil.isValidUrl(url)) {
                startDownload(url)
            } else {
                Toast.makeText(this, "الرجاء إدخال رابط صالح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDownload(urlString: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(urlString))
            val title = URLUtil.guessFileName(urlString, null, null)

            request.setTitle(title)
            request.setDescription("يتم تحميل الملف...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
            
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "بدأ التحميل...", Toast.LENGTH_SHORT).show()
            finish() // إغلاق الشاشة بعد بدء التحميل

        } catch (e: Exception) {
            Toast.makeText(this, "حدث خطأ أثناء بدء التحميل", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
