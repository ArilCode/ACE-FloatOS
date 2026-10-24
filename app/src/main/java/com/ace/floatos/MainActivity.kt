package com.ace.floatos

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    }

    override fun onResume() { super.onResume(); setContentView(buildUi()) }

    private fun tv(t: String, size: Float, color: Int = 0xFFFFFFFF.toInt(), bold: Boolean = false) =
        TextView(this).apply { text = t; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD) }

    private fun btn(t: String, color: Int, click: () -> Unit) = Button(this).apply {
        text = t; setTextColor(0xFFFFFFFF.toInt()); isAllCaps = false
        background = rounded(color, dp(14).toFloat()); setOnClickListener { click() }
    }

    private fun card(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(0x33FFFFFF, dp(18).toFloat()); setPadding(dp(16), dp(14), dp(16), dp(14))
        v.forEach { addView(it) }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) }
    }

    private fun step(n: Int, title: String, desc: String, ok: Boolean, label: String, act: () -> Unit) =
        card(tv("$n. $title  ${if (ok) "✅" else "⚠️"}", 16f, bold = true),
            tv(desc, 12f, 0xFFC8C8E8.toInt()),
            btn(if (ok) "Sudah aktif (buka pengaturan)" else label, if (ok) 0xFF2E7D6B.toInt() else 0xFF7C4DFF.toInt(), act))

    private fun open(action: String, pkg: Boolean = false) {
        val i = Intent(action); if (pkg) i.data = Uri.parse("package:$packageName"); startActivity(i)
    }

    private fun buildUi(): ScrollView {
        val root = ScrollView(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF0F0C29.toInt(), 0xFF302B63.toInt(), 0xFF24243E.toInt()))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(48), dp(20), dp(24))
        }
        col.addView(ImageView(this).apply { setImageResource(R.drawable.ic_launcher) },
            LinearLayout.LayoutParams(dp(84), dp(84)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        col.addView(tv("ACE FloatOS", 28f, bold = true).apply { gravity = Gravity.CENTER })
        col.addView(tv("Jadikan aplikasi jendela mengambang", 13f, 0xFF9E9EC8.toInt()).apply { gravity = Gravity.CENTER })

        val overlayOk = Settings.canDrawOverlays(this)
        val a11yOk = AceAccessibilityService.instance != null
        val writeOk = Settings.System.canWrite(this)

        col.addView(step(1, "Izin tampil di atas aplikasi", "Wajib, agar garis & menu bisa muncul.", overlayOk, "Berikan izin") { open(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, true) })
        col.addView(step(2, "Aksesibilitas", "Untuk tombol Screenshot & Home. Cari 'ACE FloatOS' lalu aktifkan.", a11yOk, "Buka Aksesibilitas") { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) })
        col.addView(step(3, "Ubah setelan sistem", "Untuk timeout layar (30 detik / tidak pernah).", writeOk, "Berikan izin") { open(Settings.ACTION_MANAGE_WRITE_SETTINGS, true) })
        col.addView(card(
            tv("4. Aktifkan Jendela Freeform", 16f, bold = true),
            tv("Opsi Pengembang → aktifkan 'Aktifkan jendela freeform' dan 'Paksa aktivitas dapat diubah ukurannya', lalu restart HP.", 12f, 0xFFC8C8E8.toInt()),
            btn("Buka Opsi Pengembang", 0xFF7C4DFF.toInt()) {
                try { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }
                catch (_: Exception) { Toast.makeText(this, "Aktifkan dulu: Tentang ponsel → ketuk Nomor build 7x", Toast.LENGTH_LONG).show() }
            }))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(btn("▶ Mulai", 0xFF00A86B.toInt()) {
            if (!Settings.canDrawOverlays(this)) Toast.makeText(this, "Berikan izin langkah 1 dulu", Toast.LENGTH_SHORT).show()
            else startForegroundService(Intent(this, OverlayService::class.java))
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        row.addView(btn("■ Stop", 0xFFD32F2F.toInt()) { stopService(Intent(this, OverlayService::class.java)) },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        col.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        col.addView(tv("Pilih aplikasi untuk menu mengambang", 16f, bold = true).apply { setPadding(0, dp(24), 0, dp(4)) })
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        val chosen = Prefs.get(this)
        for (r in apps) {
            val pkg = r.activityInfo.packageName
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
            }
            line.addView(ImageView(this).apply { setImageDrawable(r.loadIcon(pm)) }, LinearLayout.LayoutParams(dp(36), dp(36)))
            line.addView(tv(r.loadLabel(pm).toString(), 15f).apply { setPadding(dp(12), 0, 0, 0) },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            line.addView(CheckBox(this).apply {
                isChecked = pkg in chosen
                setOnCheckedChangeListener { _, on -> Prefs.set(this@MainActivity, pkg, on) }
            })
            col.addView(line)
        }

        col.addView(tv("ACE (ArilCode Engineer)", 12f, 0xFF7C7CA8.toInt()).apply { gravity = Gravity.CENTER; setPadding(0, dp(28), 0, 0) })
        root.addView(col)
        return root
    }
}
