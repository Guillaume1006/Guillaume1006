package com.guillaume.ioslauncher

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.min

class MainActivity : Activity() {

    private class App(
        val label: String,
        val component: ComponentName,
        val icon: Drawable,
        val category: Int,
    ) {
        val pkg: String get() = component.packageName
    }

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var pager: ViewPager
    private lateinit var dots: LinearLayout
    private lateinit var dock: LinearLayout
    private lateinit var search: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var searchResults: LinearLayout
    private lateinit var detector: GestureDetector

    private var apps = emptyList<App>()
    private val pages = ArrayList<View>()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        buildUi()

        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (vy > 1200 && dy > dp(80) && abs(e2.x - e1.x) < dy) {
                    openSearch()
                    return true
                }
                return false
            }
        })

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageReceiver, filter)
        }
        reload()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (search.visibility == View.VISIBLE) closeSearch()
        pager.setCurrentItem(0, true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (search.visibility == View.VISIBLE) closeSearch()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (search.visibility != View.VISIBLE) detector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ---------------------------------------------------------------- UI

    private fun buildUi() {
        val root = FrameLayout(this)

        pager = ViewPager(this)
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(0x59FFFFFF, 32)
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(pager, LinearLayout.LayoutParams(match, 0, 1f))
            addView(dots, LinearLayout.LayoutParams(match, dp(24)))
            addView(dock, LinearLayout.LayoutParams(match, dp(88)).apply {
                setMargins(dp(12), 0, dp(12), dp(8))
            })
        }
        root.addView(main, FrameLayout.LayoutParams(match, match))

        searchInput = EditText(this).apply {
            hint = "Rechercher"
            setHintTextColor(0x99FFFFFF.toInt())
            setTextColor(Color.WHITE)
            textSize = 17f
            isSingleLine = true
            background = rounded(0x40FFFFFF, 14)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = runSearch(s?.toString().orEmpty())
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        searchResults = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        search = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundColor(0xD9000000.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            addView(searchInput, LinearLayout.LayoutParams(match, wrap))
            addView(
                ScrollView(context).apply { addView(searchResults) },
                LinearLayout.LayoutParams(match, 0, 1f).apply { topMargin = dp(12) },
            )
        }
        root.addView(search, FrameLayout.LayoutParams(match, match))

        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            main.setPadding(0, bars.top, 0, bars.bottom)
            search.setPadding(dp(16), bars.top + dp(16), dp(16), bars.bottom + dp(16))
            insets
        }

        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
        setContentView(root)
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun reload() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                App(
                    it.loadLabel(pm).toString(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                    it.loadIcon(pm),
                    it.activityInfo.applicationInfo.category,
                )
            }
            .sortedBy { it.label.lowercase() }

        val dockApps = pickDock()
        val rest = apps.filter { a -> dockApps.none { it.pkg == a.pkg } }

        pages.clear()
        var i = 0
        do {
            val header = pages.isEmpty()
            val capacity = if (header) 16 else 20
            pages.add(makePage(rest.subList(i, min(i + capacity, rest.size)), header))
            i += capacity
        } while (i < rest.size)
        pages.add(makeLibrary())

        dock.removeAllViews()
        dockApps.forEach { dock.addView(iconCell(it, 60, false), LinearLayout.LayoutParams(0, match, 1f)) }

        val current = pager.currentItem
        pager.adapter = object : PagerAdapter() {
            override fun getCount() = pages.size
            override fun isViewFromObject(view: View, obj: Any) = view === obj
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val v = pages[position]
                (v.parent as? ViewGroup)?.removeView(v)
                container.addView(v)
                return v
            }
            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) =
                container.removeView(obj as View)
        }
        pager.currentItem = min(current, pages.size - 1)
        updateDots(pager.currentItem)
    }

    private fun updateDots(selected: Int) {
        dots.removeAllViews()
        for (i in pages.indices) {
            val dot = View(this).apply {
                background = rounded(if (i == selected) Color.WHITE else 0x66FFFFFF, 4)
            }
            dots.addView(dot, LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                setMargins(dp(4), 0, dp(4), 0)
            })
        }
    }

    private fun pickDock(): List<App> {
        val pm = packageManager
        val wanted = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        val result = ArrayList<App>()
        for (w in wanted) {
            val pkg = pm.resolveActivity(w, 0)?.activityInfo?.packageName ?: continue
            val app = apps.firstOrNull { it.pkg == pkg } ?: continue
            if (result.none { it.pkg == app.pkg }) result.add(app)
        }
        for (app in apps) {
            if (result.size >= 4) break
            if (result.none { it.pkg == app.pkg }) result.add(app)
        }
        return result.take(4)
    }

    private fun makePage(items: List<App>, header: Boolean): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), 0)
        }
        if (header) {
            page.addView(clockCard(), LinearLayout.LayoutParams(match, 0, 1f).apply {
                setMargins(dp(8), 0, dp(8), dp(12))
            })
        }
        val rows = if (header) 4 else 5
        for (r in 0 until rows) {
            val row = LinearLayout(this)
            for (c in 0 until 4) {
                val idx = r * 4 + c
                val cell = if (idx < items.size) iconCell(items[idx], 60, true) else View(this)
                row.addView(cell, LinearLayout.LayoutParams(0, match, 1f))
            }
            page.addView(row, LinearLayout.LayoutParams(match, 0, 1f))
        }
        return page
    }

    private fun clockCard(): View {
        val time = TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "h:mm"
            setTextColor(Color.WHITE)
            textSize = 48f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        }
        val date = TextClock(this).apply {
            format24Hour = "EEEE d MMMM"
            format12Hour = "EEEE d MMMM"
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 16f
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(0x40000000, 24)
            addView(time)
            addView(date)
        }
    }

    private fun makeLibrary(): View {
        val groups = sortedMapOf<String, MutableList<App>>()
        for (a in apps) {
            @Suppress("DEPRECATION")
            val name = when (a.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                ApplicationInfo.CATEGORY_GAME -> "Jeux"
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> "Divertissement"
                ApplicationInfo.CATEGORY_PRODUCTIVITY,
                ApplicationInfo.CATEGORY_NEWS -> "Productivité et infos"
                ApplicationInfo.CATEGORY_MAPS -> "Voyages"
                else -> "Utilitaires"
            }
            groups.getOrPut(name) { ArrayList() }.add(a)
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        col.addView(TextView(this).apply {
            text = "Bibliothèque d'apps"
            setTextColor(Color.WHITE)
            textSize = 26f
            setPadding(dp(4), dp(8), 0, dp(12))
        })

        val entries = groups.entries.toList()
        for (i in entries.indices step 2) {
            val row = LinearLayout(this)
            for (j in i until min(i + 2, entries.size)) {
                row.addView(libraryCard(entries[j].key, entries[j].value), LinearLayout.LayoutParams(0, wrap, 1f).apply {
                    setMargins(dp(6), dp(6), dp(6), dp(10))
                })
            }
            col.addView(row)
        }
        return ScrollView(this).apply { addView(col) }
    }

    private fun libraryCard(title: String, items: List<App>): View {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(0x59FFFFFF, 24)
        }
        val shown = items.take(4)
        for (r in 0 until 2) {
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            for (c in 0 until 2) {
                val app = shown.getOrNull(r * 2 + c)
                val cell: View = if (app != null) {
                    iconView(app, 34).also { v ->
                        v.setOnClickListener { launch(app, v) }
                        v.setOnLongClickListener { showMenu(app); true }
                    }
                } else View(this)
                row.addView(cell, LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    setMargins(dp(8), dp(8), dp(8), dp(8))
                })
            }
            grid.addView(row)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(grid, LinearLayout.LayoutParams(match, wrap))
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    // ------------------------------------------------------------ icons

    private fun iconView(app: App, sizeDp: Int): ImageView {
        val drawable = app.icon.constantState?.newDrawable()?.mutate() ?: app.icon
        return ImageView(this).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.FIT_XY
            if (drawable !is AdaptiveIconDrawable) {
                setBackgroundColor(Color.WHITE)
                val pad = dp(sizeDp) / 8
                setPadding(pad, pad, pad, pad)
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.225f)
                }
            }
        }
    }

    private fun iconCell(app: App, sizeDp: Int, label: Boolean): View {
        val icon = iconView(app, sizeDp)
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(icon, LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)))
            if (label) {
                addView(TextView(context).apply {
                    text = app.label
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                    setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
                    setPadding(dp(2), dp(5), dp(2), 0)
                })
            }
            setOnClickListener { launch(app, icon) }
            setOnLongClickListener { showMenu(app); true }
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false
            }
        }
        return cell
    }

    private fun launch(app: App, from: View) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val options = ActivityOptions.makeScaleUpAnimation(from, 0, 0, from.width, from.height)
        try {
            startActivity(intent, options.toBundle())
        } catch (_: Exception) {
        }
    }

    private fun showMenu(app: App) {
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(arrayOf("Infos de l'app", "Désinstaller")) { _, which ->
                val uri = Uri.parse("package:${app.pkg}")
                val action = if (which == 0) Settings.ACTION_APPLICATION_DETAILS_SETTINGS else Intent.ACTION_DELETE
                startActivity(Intent(action, uri))
            }
            .show()
    }

    // ----------------------------------------------------------- search

    private fun openSearch() {
        searchInput.setText("")
        runSearch("")
        search.visibility = View.VISIBLE
        searchInput.requestFocus()
        searchInput.post {
            getSystemService(InputMethodManager::class.java)
                .showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun closeSearch() {
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(searchInput.windowToken, 0)
        search.visibility = View.GONE
    }

    private fun runSearch(query: String) {
        searchResults.removeAllViews()
        apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }.forEach { app ->
            val icon = iconView(app, 44)
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(8), dp(4), dp(8))
                addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(TextView(context).apply {
                    text = app.label
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    setPadding(dp(14), 0, 0, 0)
                })
                setOnClickListener {
                    closeSearch()
                    launch(app, icon)
                }
            }
            searchResults.addView(row, LinearLayout.LayoutParams(match, wrap))
        }
    }
}
