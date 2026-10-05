package com.example.htmlkeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class MyKeyboardService : InputMethodService() {
    private var webView: WebView? = null
    private lateinit var layout: LinearLayout
    private var lang = "RU"
    private var shift = false
    private var symbolMode = false

    private val layouts = mapOf(
        "RU" to listOf(listOf("й","ц","у","к","е","н","г","ш","щ","з","х","ъ"), listOf("ф","ы","в","а","п","р","о","л","д","ж","э"), listOf("⇧","я","ч","с","м","и","т","ь","б","ю","⌫"), listOf("🌐","Пробел","Enter")),
        "EN" to listOf(listOf("q","w","e","r","t","y","u","i","o","p"), listOf("a","s","d","f","g","h","j","k","l"), listOf("⇧","z","x","c","v","b","n","m","⌫"), listOf("🌐","Пробел","Enter"))
    )

    private val symbolLayout = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("-", "/", ":", ";", "(", ")", "₽", "&", "@", "\""),
        listOf(".", ",", "?", "!", "'", "[", "]", "{", "}", "⌫"),
        listOf("🌐", "Пробел", "Enter")
    )

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, (280 * density).toInt())
        }

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(-1, -1).apply { topMargin = (45 * density).toInt() }
            background = android.graphics.drawable.ColorDrawable(0xff121212.toInt())
            setPadding(4, 4, 4, 4)
        }
        container.addView(layout)
        renderKeys()

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, (45 * density).toInt(), Gravity.TOP)
            isFocusable = false; clearFocus()
            
            clearCache(true)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            
            addJavascriptInterface(this@MyKeyboardService, "AndroidKeyboard")
        }
        container.addView(webView)
        Handler(Looper.getMainLooper()).postDelayed({ webView?.loadUrl("file:///android_asset/index.html") }, 30)

        return container
    }

    private fun renderKeys() {
        layout.removeAllViews()
        val isTab = resources.configuration.screenWidthDp >= 600
        val m = if (isTab) 4 else 2
        val currentRows = if (symbolMode) symbolLayout else layouts[lang]!!

        for (rowKeys in currentRows) {
            val row = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
            for (k in rowKeys) {
                row.addView(Button(this).apply {
                    text = if (shift && k.length == 1 && k.first().isLetter()) k.uppercase() else k
                    layoutParams = LinearLayout.LayoutParams(0, -1, when(k){ "Пробел"->4f; "Enter","⇧","⌫"->1.5f; else->1f }).apply { setMargins(m, m, m, m) }
                    setBackgroundColor(if(k=="⇧"&&shift) 0xff444444.toInt() else if(listOf("⇧","⌫","Enter","🌐").contains(k)) 0xff1e1e1e.toInt() else 0xff2d2d2d.toInt())
                    setTextColor(-1); textSize = if (isTab) 20f else 16f; isAllCaps = false
                    setOnClickListener { handleKey(k) }
                })
            }
            layout.addView(row)
        }
    }

    private fun handleKey(k: String) {
        val ic = currentInputConnection ?: return
        when (k) {
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "Enter" -> { ic.sendKeyEvent(android.view.KeyEvent(0, 66)); ic.sendKeyEvent(android.view.KeyEvent(1, 66)) }
            "Пробел" -> ic.commitText(" ", 1)
            "⇧" -> { shift = !shift; renderKeys() }
            "🌐" -> { 
                if (symbolMode) {
                    symbolMode = false
                    webView?.evaluateJavascript("updateSymbolButtonState(false);", null)
                } else {
                    lang = if (lang == "RU") "EN" else "RU"
                    webView?.evaluateJavascript("updateLangIndicator('$lang');", null)
                }
                renderKeys()
            }
            else -> { ic.commitText(if(shift) k.uppercase() else k, 1); if(shift){ shift = false; renderKeys() } }
        }
    }

    @JavascriptInterface fun commitText(t: String) { Handler(Looper.getMainLooper()).post { currentInputConnection?.commitText(t, 1) } }
    @JavascriptInterface fun toggleSymbolMode(enable: Boolean) { Handler(Looper.getMainLooper()).post { symbolMode = enable; renderKeys() } }
    @JavascriptInterface fun toggleWidgetView(ex: Boolean) {
        Handler(Looper.getMainLooper()).post {
            val p = webView?.layoutParams as? FrameLayout.LayoutParams ?: return@post
            p.height = if (ex) -1 else (45 * resources.displayMetrics.density).toInt()
            webView?.layoutParams = p
        }
    }
    override fun onDestroy() { webView?.destroy(); super.onDestroy() }
}
