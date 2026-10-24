package com.ace.floatos

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.widget.Toast

object FreeformLauncher {
    private var cascade = 0

    fun launch(ctx: Context, pkg: String) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val dm = ctx.resources.displayMetrics
        val w = (dm.widthPixels * 0.85).toInt()
        val h = (dm.heightPixels * 0.55).toInt()
        val off = (cascade % 5) * ctx.dp(36); cascade++
        val left = (dm.widthPixels - w) / 2 - ctx.dp(36) + off
        val top = ctx.dp(80) + off
        val opts = ActivityOptions.makeBasic()
        opts.setLaunchBounds(Rect(left, top, left + w, top + h))
        try { // 5 = WINDOWING_MODE_FREEFORM (hidden API, dicoba saja)
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(opts, 5)
        } catch (_: Throwable) {}
        try {
            ctx.startActivity(intent, opts.toBundle())
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membuka: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
