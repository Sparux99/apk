package com.amine.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.amine.player.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var binding: ActivitySettingsBinding
    private var themeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) طبّق الثيم المحفوظ قبل setContentView
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        setTheme(ThemeHelper.getSavedTheme(this))

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "الإعدادات"

        setupColorSelection()
        setupSeekTimeSelection()
        setupRememberPositionSwitch()
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
                setResult(RESULT_OK)
                // إعادة رسم شاشة الإعدادات نفسها لعرض اللون الجديد فوراً:
                recreate()
            }
        }
    }

    private fun setupSeekTimeSelection() {
        val seekRadioGroup = binding.seekTimeRadioGroup
        val currentSeekTime = prefs.getInt("SeekTime", 10_000)
        when (currentSeekTime) {
            5_000 -> seekRadioGroup.check(R.id.seek_5s)
            15_000 -> seekRadioGroup.check(R.id.seek_15s)
            else -> seekRadioGroup.check(R.id.seek_10s)
        }

        seekRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val seekTime = when (checkedId) {
                R.id.seek_5s -> 5_000
                R.id.seek_15s -> 15_000
                else -> 10_000
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (themeChanged) setResult(RESULT_OK) else setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
