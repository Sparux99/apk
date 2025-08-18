package com.amine.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    // SharedPreferences لتخزين الإعدادات
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // قم بتطبيق الثيم المختار قبل عرض الواجهة
        setTheme(getAppTheme())
        setContentView(R.layout.activity_settings)
        // تفعيل زر العودة في شريط التطبيق
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "الإعدادات"

        // الحصول على SharedPreferences لتخزين واسترجاع الإعدادات
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // إعداد جميع أقسام الإعدادات
        setupColorSelection()
        setupSeekTimeSelection()
        setupRememberPositionSwitch()
    }

    // إعداد خيارات اختيار ألوان الواجهة
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
            // حفظ الثيم المختار وإعادة إنشاء النشاط لتطبيقه
            prefs.edit().putInt("AppTheme", themeId).apply()
            recreate()
        }
    }

    // إعداد خيارات التحكم في وقت التقديم والتأخير
    private fun setupSeekTimeSelection() {
        val seekRadioGroup = findViewById<RadioGroup>(R.id.seek_time_radio_group)
        // الحصول على وقت التخطي الحالي، والقيمة الافتراضية 10 ثوانٍ (10000ms)
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
            // حفظ وقت التخطي المختار
            prefs.edit().putInt("SeekTime", seekTime).apply()
        }
    }

    // إعداد مفتاح التبديل لتذكر آخر موقع
    private fun setupRememberPositionSwitch() {
        val rememberPositionSwitch = findViewById<Switch>(R.id.remember_position_switch)
        // الحصول على حالة الخيار، والقيمة الافتراضية true (مفعل)
        val isRememberEnabled = prefs.getBoolean("RememberPosition", true)
        rememberPositionSwitch.isChecked = isRememberEnabled

        rememberPositionSwitch.setOnCheckedChangeListener { _, isChecked ->
            // حفظ حالة الخيار الجديدة
            prefs.edit().putBoolean("RememberPosition", isChecked).apply()
        }
    }

    // دالة للحصول على الثيم من SharedPreferences
    private fun getAppTheme(): Int {
        return getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getInt("AppTheme", R.style.Theme_Amine)
    }

    // التحكم في زر العودة في شريط التطبيق
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}