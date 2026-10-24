package com.ace.floatos

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private val ui = Handler(Looper.getMainLooper())

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startFg()
        showHandle()
    }

    override fun onDestroy() {
        hidePanel()
        handle?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ace", "ACE FloatOS", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "ace")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("ACE FloatOS aktif")
            .setContentText("Geser garis di tepi kanan layar untuk membuka menu")
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun overlayParams(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
    )

    // ---------- GARIS PEMICU DI TEPI LAYAR ----------
    private fun showHandle() {
        val box = FrameLayout(this)
        val bar = View(this).apply { background = rounded(0xCC7C4DFF.toInt(), dp(3).toFloat()) }
        box.addView(bar, FrameLayout.LayoutParams(dp(6), MATCH_PARENT, Gravity.END))
        var startX = 0f
        var opened = false
        box.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = e.rawX; opened = false }
                MotionEvent.ACTION_MOVE -> if (!opened && startX - e.rawX > dp(20)) { opened = true; showPanel() }
                MotionEvent.ACTION_UP -> if (!opened && Math.abs(startX - e.rawX) < dp(10)) showPanel()
            }
            true
        }
        val lp = overlayParams(dp(28), dp(130),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        wm.addView(box, lp)
        handle = box
    }

    // ---------- PANEL MENU ----------
    private fun showPanel() {
        if (panel != null) return
        val scrim = FrameLayout(this).apply {
            setBackgroundColor(0x66000000); setOnClickListener { hidePanel() }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xF2151522.toInt(), dp(24).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(12))
            isClickable = true
        }
        card.addView(text("ACE FloatOS", 20f, 0xFFFFFFFF.toInt(), true))
        card.addView(text("Ketuk aplikasi untuk dibuka sebagai jendela", 12f, 0xFF9E9EC8.toInt(), false))

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pm = packageManager
        for (pkg in Prefs.get(this)) {
            val info = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { continue }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(0xFF22223A.toInt(), dp(14).toFloat())
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener { hidePanel(); ui.postDelayed({ FreeformLauncher.launch(this@OverlayService, pkg) }, 150) }
            }
            row.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(pm)) }, LinearLayout.LayoutParams(dp(40), dp(40)))
            row.addView(text(info.loadLabel(pm).toString(), 15f, 0xFFFFFFFF.toInt(), false).apply { setPadding(dp(12), 0, 0, 0) })
            list.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        }
        // tombol +
        val add = text("+  Tambah aplikasi", 15f, 0xFF00E5FF.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF1B2A3A.toInt(), dp(14).toFloat())
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener {
                hidePanel()
                startActivity(Intent(this@OverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        list.addView(add, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })
        card.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        // tools
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(tool("📸", "Shot") { act { global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) } })
        tools.addView(tool("⏱", "30 dtk") { timeout(30_000) })
        tools.addView(tool("♾", "Selalu") { timeout(Int.MAX_VALUE) })
        tools.addView(tool("🏠", "Home") { act { global(AccessibilityService.GLOBAL_ACTION_HOME) } })
        tools.addView(tool("⏺", "Rekam") { toast("Perekam layar: tahap berikutnya") })
        card.addView(tools, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        card.addView(text("ACE (ArilCode Engineer)", 11f, 0xFF7C7CA8.toInt(), false).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        scrim.addView(card, FrameLayout.LayoutParams(dp(300), MATCH_PARENT, Gravity.END).apply {
            setMargins(dp(8), dp(48), dp(8), dp(48))
        })
        wm.addView(scrim, overlayParams(MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE))
        panel = scrim
    }

    private fun hidePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun global(action: Int) {
        val s = AceAccessibilityService.instance
        if (s == null) toast("Aktifkan Aksesibilitas ACE dulu") else s.performGlobalAction(action)
    }

    private fun act(block: () -> Unit) { hidePanel(); ui.postDelayed({ block() }, 500) }

    private fun timeout(ms: Int) {
        if (!Settings.System.canWrite(this)) {
            toast("Izinkan 'Ubah setelan sistem' dulu")
            hidePanel()
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
        toast(if (ms == Int.MAX_VALUE) "Layar tidak akan padam" else "Layar padam 30 detik")
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun text(t: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun tool(emoji: String, label: String, click: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(0xFF22223A.toInt(), dp(12).toFloat())
        setPadding(0, dp(8), 0, dp(8))
        addView(text(emoji, 20f, 0xFFFFFFFF.toInt(), false).apply { gravity = Gravity.CENTER })
        addView(text(label, 10f, 0xFFB0B0D0.toInt(), false).apply { gravity = Gravity.CENTER })
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
    }
}
