package com.amine.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(getAppTheme()) // تطبيق الثيم قبل عرض الواجهة
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "الإعدادات"

        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        setupColorSelection()
        setupSeekTimeSelection()
    }

    private fun setupColorSelection() {
        val colorRadioGroup = findViewById<RadioGroup>(R.id.color_radio_group)
        val currentTheme = prefs.getInt("AppTheme", R.style.Theme_Amine)
        when (currentTheme) {
            R.style.Theme_Amine_Blue -> colorRadioGroup.check(R.id.color_blue)
            R.style.Theme_Amine_Green -> colorRadioGroup.check(R.id.color_green)
            else -> colorRadioGroup.check(R.id.color_default)
        }

        colorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeId = when (checkedId) {
                R.id.color_blue -> R.style.Theme_Amine_Blue
                R.id.color_green -> R.style.Theme_Amine_Green
                else -> R.style.Theme_Amine
            }
            prefs.edit().putInt("AppTheme", themeId).apply()
            recreate() // إعادة إنشاء الواجهة لتطبيق الثيم الجديد
        }
    }
    
    private fun setupSeekTimeSelection() {
        val seekRadioGroup = findViewById<RadioGroup>(R.id.seek_time_radio_group)
        val currentSeekTime = prefs.getInt("SeekTime", 10000) // 10s default
        when (currentSeekTime) {
            5000 -> seekRadioGroup.check(R.id.seek_5s)
            15000 -> seekRadioGroup.check(R.id.seek_15s)
            else -> seekRadioGroup.check(R.id.seek_10s)
        }
        
        seekRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val seekTime = when (checkedId) {
                R.id.seek_5s -> 5000
                R.id.seek_15s -> 15000
                else -> 10000
            }
            prefs.edit().putInt("SeekTime", seekTime).apply()
        }
    }
    
    private fun getAppTheme(): Int {
        return getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getInt("AppTheme", R.style.Theme_Amine)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}