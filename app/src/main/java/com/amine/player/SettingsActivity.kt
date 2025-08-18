package com.amine.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amine.player.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var binding: ActivitySettingsBinding
    private var themeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // خذ الإعدادات أولاً حتى نطبق الثيم قبل إنشاء الـ Activity
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val currentTheme = getAppTheme()
        setTheme(currentTheme)

        super.onCreate(savedInstanceState)

        // ViewBinding inflate
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "الإعدادات"

        setupColorSelection()
        setupSeekTimeSelection()
        setupRememberPositionSwitch()
    }

    // في ملف SettingsActivity.kt
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // تحقق إذا كان العنصر الذي تم الضغط عليه هو زر العودة
        if (item.itemId == android.R.id.home) {
            // إذا كان كذلك، أغلق النشاط الحالي (SettingsActivity)
            finish() 
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupColorSelection() {
        val colorRadioGroup = binding.colorRadioGroup
        val currentTheme = prefs.getInt("AppTheme", R.style.Theme_Amine)

        when (currentTheme) {
            R.style.Theme_Amine_Blue -> colorRadioGroup.check(R.id.color_blue)
            R.style.Theme_Amine_Green -> colorRadioGroup.check(R.id.color_green)
            R.style.Theme_Amine_Red -> colorRadioGroup.check(R.id.color_red)
            R.style.Theme_Amine_Orange -> colorRadioGroup.check(R.id.color_orange)
            R.style.Theme_Amine_Teal -> colorRadioGroup.check(R.id.color_teal)
            R.style.Theme_Amine_Pink -> colorRadioGroup.check(R.id.color_pink)
            else -> colorRadioGroup.check(R.id.color_default)
        }

        colorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeId = when (checkedId) {
                R.id.color_blue -> R.style.Theme_Amine_Blue
                R.id.color_green -> R.style.Theme_Amine_Green
                R.id.color_red -> R.style.Theme_Amine_Red
                R.id.color_orange -> R.style.Theme_Amine_Orange
                R.id.color_teal -> R.style.Theme_Amine_Teal
                R.id.color_pink -> R.style.Theme_Amine_Pink
                else -> R.style.Theme_Amine
            }

            val old = prefs.getInt("AppTheme", R.style.Theme_Amine)
            if (old != themeId) {
                prefs.edit().putInt("AppTheme", themeId).apply()
                themeChanged = true
                setResult(RESULT_OK) // إعلام MainActivity بوجود تغيير لتهيئة الثيم عند العودة
            }
            // لا نقوم بـ recreate() هنا لتجنب وميض الـ RadioButtons
        }
    }

    private fun setupSeekTimeSelection() {
        val seekRadioGroup = binding.seekTimeRadioGroup
        val currentSeekTime = prefs.getInt("SeekTime", 10000)
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

    private fun setupRememberPositionSwitch() {
        val rememberSwitch = binding.rememberPositionSwitch
        val isRememberEnabled = prefs.getBoolean("RememberPosition", true)
        rememberSwitch.isChecked = isRememberEnabled

        rememberSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("RememberPosition", isChecked).apply()
        }
    }

    private fun getAppTheme(): Int {
        return prefs.getInt("AppTheme", R.style.Theme_Amine)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (themeChanged) setResult(RESULT_OK) else setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
