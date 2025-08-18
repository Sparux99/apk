package com.amine.player

import android.content.Context

object ThemeHelper {
    fun getSavedTheme(context: Context): Int {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getInt("AppTheme", R.style.Theme_Amine)
    }
}
