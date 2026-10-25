package com.example.ioslauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.Executors

/** Icône déjà rasterisée à la taille finale : aucun travail de dessin coûteux pendant le défilement. */
class App(val label: String, val icon: Bitmap, val intent: Intent, val pkg: String)

class MainActivity : Activity() {

    private lateinit var pager: ViewPager2
    private lateinit var dots: LinearLayout
    private lateinit var dock: LinearLayout
    private var allApps: List<App> = emptyList()
    private var dockApps: List<App> = emptyList()
    private var pages: List<List<App>> = emptyList()
    private val cols = 4
    private var rows = 6

    private val worker = Executors.newSingleThreadExecutor()
    private val iconCache = HashMap<String, Bitmap>()
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = loadApps()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val iconPx by lazy { dp(ICON_DP) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge (imposé par Android 15/16) + barre de gestes Nothing OS transparente
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        pager = ViewPager2(this)
        pager.adapter = PageAdapter()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
        pager.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) refreshPages()
        }
        root.addView(pager, LinearLayout.LayoutParams(-1, 0, 1f))

        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(dots, LinearLayout.LayoutParams(-1, dp(20)))

        val search = TextView(this).apply {
            text = "🔍  Rechercher"
            setTextColor(Color.WHITE); textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(8), dp(28), dp(8))
            background = pill(0x40FFFFFF, 20)
            setOnClickListener {
                if (!startSafe(Intent(Intent.ACTION_WEB_SEARCH))) {
                    startSafe(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER))
                }
            }
        }
        root.addView(search, LinearLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12)
        })

        dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(12), dp(10), dp(12))
            background = pill(0x59FFFFFF, 34)
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(12), 0, dp(12), dp(8))
        })
        setContentView(root)

        // Retour prédictif (Android 13+, obligatoire avec targetSdk 36) : onBackPressed n'est plus appelé
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                pager.currentItem = 0
            }
        }

        // Rechargement uniquement quand une app est installée / mise à jour / supprimée
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        loadApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
        worker.shutdown()
    }

    @Deprecated("Launcher: retour = première page (Android < 13)")
    override fun onBackPressed() { pager.currentItem = 0 }

    /** Adapte le nombre de lignes à la hauteur réelle de l'écran. */
    private fun refreshPages() {
        val avail = pager.height - dp(16)
        val newRows = if (avail > 0) (avail / dp(ROW_DP)).coerceIn(3, 10) else rows
        if (newRows == rows && pages.isNotEmpty()) return
        rows = newRows
        pages = allApps.chunked(cols * rows)
        pager.adapter?.notifyDataSetChanged()
        updateDots(pager.currentItem.coerceAtMost((pages.size - 1).coerceAtLeast(0)))
    }

    /** Charge apps + icônes hors du thread UI, avec cache des icônes déjà rasterisées. */
    private fun loadApps() {
        worker.execute {
            val pm = packageManager
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(q, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map {
                    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(it.activityInfo.packageName, it.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val key = it.activityInfo.packageName + "/" + it.activityInfo.name
                    val icon = iconCache.getOrPut(key) { render(it.loadIcon(pm)) }
                    App(it.loadLabel(pm).toString(), icon, i, it.activityInfo.packageName)
                }.sortedBy { it.label.lowercase() }

            val dockIntents = listOf(
                Intent(Intent.ACTION_DIAL),
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            )
            val dockList = dockIntents.mapNotNull { i ->
                val r = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) ?: return@mapNotNull null
                if (r.activityInfo.packageName == "android") return@mapNotNull null
                val key = "dock/" + r.activityInfo.packageName + "/" + r.activityInfo.name
                val icon = iconCache.getOrPut(key) { render(r.loadIcon(pm)) }
                App("", icon, i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), r.activityInfo.packageName)
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                allApps = list; dockApps = dockList
                buildDock()
                rows = -1 // force le recalcul
                refreshPages()
            }
        }
    }

    /**
     * Rasterise l'icône à la taille finale avec le même masque arrondi pour toutes les apps
     * (icônes adaptatives ET anciennes icônes), afin d'obtenir un rendu homogène.
     */
    private fun render(d: Drawable): Bitmap {
        val s = iconPx
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (d is AdaptiveIconDrawable) {
            val o = (s * 0.25f).toInt() // les calques font 108dp pour 72dp visibles
            d.background?.let { it.setBounds(-o, -o, s + o, s + o); it.draw(c) }
            d.foreground?.let { it.setBounds(-o, -o, s + o, s + o); it.draw(c) }
        } else {
            c.drawColor(0xFFF2F2F7.toInt())
            val pad = (s * 0.12f).toInt()
            d.setBounds(pad, pad, s - pad, s - pad); d.draw(c)
        }
        // Masque arrondi : drawBitmap couvre tout le carré, donc DST_IN efface bien les coins
        val r = s * 0.225f
        val mask = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        Canvas(mask).drawRoundRect(RectF(0f, 0f, s.toFloat(), s.toFloat()), r, r, Paint(Paint.ANTI_ALIAS_FLAG))
        c.drawBitmap(mask, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        mask.recycle()
        return bmp
    }

    private fun buildDock() {
        dock.removeAllViews()
        for (app in dockApps) dock.addView(cell(app, false), LinearLayout.LayoutParams(dp(72), -2))
    }

    private fun updateDots(sel: Int) {
        dots.removeAllViews()
        if (pages.size < 2) return
        for (i in pages.indices) {
            val d = View(this)
            d.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == sel) Color.WHITE else 0x66FFFFFF)
            }
            dots.addView(d, LinearLayout.LayoutParams(dp(7), dp(7)).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
    }

    private fun pill(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun startSafe(i: Intent): Boolean = try {
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: Exception) { false }

    private fun cell(app: App, label: Boolean): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val iv = ImageView(this).apply { setImageBitmap(app.icon) }
        c.addView(iv, LinearLayout.LayoutParams(iconPx, iconPx))
        if (label) {
            c.addView(TextView(this).apply {
                text = app.label; setTextColor(Color.WHITE); textSize = 10f
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
            }, LinearLayout.LayoutParams(dp(68), -2).apply { topMargin = dp(3) })
        }
        c.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
        c.setOnClickListener { startSafe(app.intent) }
        c.setOnLongClickListener {
            startSafe(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
            true
        }
        return c
    }

    inner class PageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = pages.size
        override fun onCreateViewHolder(p: ViewGroup, t: Int): RecyclerView.ViewHolder {
            val page = LinearLayout(p.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(16), dp(8), 0)
                layoutParams = ViewGroup.LayoutParams(-1, -1)
            }
            return object : RecyclerView.ViewHolder(page) {}
        }
        override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
            val page = h.itemView as LinearLayout
            page.removeAllViews()
            for (row in pages[pos].chunked(cols)) {
                val r = LinearLayout(page.context)
                for (k in 0 until cols) {
                    val a = row.getOrNull(k)
                    r.addView(if (a != null) cell(a, true) else View(page.context), LinearLayout.LayoutParams(0, -2, 1f))
                }
                page.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
            }
        }
    }

    private companion object {
        const val ICON_DP = 48
        const val ROW_DP = 92 // 6+48+3+~13+6 (cellule) + 12 (marge)
    }
}
