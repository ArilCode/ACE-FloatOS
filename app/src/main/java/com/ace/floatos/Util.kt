package com.ace.floatos

import android.content.Context
import android.graphics.drawable.GradientDrawable

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
    setColor(color); cornerRadius = radius
}

object Prefs {
    private const val K = "apps"
    fun get(c: Context): List<String> =
        c.getSharedPreferences("ace", 0).getString(K, "")!!.split(",").filter { it.isNotBlank() }

    fun set(c: Context, pkg: String, on: Boolean) {
        val l = get(c).toMutableList()
        l.remove(pkg); if (on) l.add(pkg)
        c.getSharedPreferences("ace", 0).edit().putString(K, l.joinToString(",")).apply()
    }
}
