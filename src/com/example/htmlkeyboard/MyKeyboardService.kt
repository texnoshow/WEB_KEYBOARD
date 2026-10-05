package com.example.htmlkeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class MyKeyboardService : InputMethodService() {

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var container: FrameLayout
    private lateinit var nativeKeyboardLayout: LinearLayout

    // Состояния клавиатуры
    private var currentLanguage = "RU"
    private var isShiftActive = false

    // Константы размеров (Повысили общую высоту до 280dp)
    private val keyboardHeightDp = 280
    private val topBarHeightDp = 45

    // Двухъязыковые системные раскладки клавиш
    private val layouts = mapOf(
        "RU" to listOf(
            listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
            listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
            listOf("⇧", "я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "⌫"),
            listOf("🌐", "Пробел", "Enter")
        ),
        "EN" to listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf("🌐", "Пробел", "Enter")
        )
    )

    override fun onCreateInputView(): View {
        val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)
        val density = resources.displayMetrics.density

        // Главный контейнер (Повышенный размер под смартфоны и планшеты)
        container = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (keyboardHeightDp * density).toInt()
            )
        }

        // Контейнер для нативных клавиш
        nativeKeyboardLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { 
                topMargin = (topBarHeightDp * density).toInt() 
            }
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#121212"))
            padding = 4
        }
        container.addView(nativeKeyboardLayout)

        // Отрисовка клавиш
        renderNativeKeys(themedContext)

        // Веб-интерфейс для виджетов (Полоска 45dp сверху)
        webView = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (topBarHeightDp * density).toInt()
            ).apply { gravity = Gravity.TOP }

            isFocusable = false
            isFocusableInTouchMode = false
            clearFocus()

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true

            addJavascriptInterface(WebAppInterface(this@MyKeyboardService), "AndroidKeyboard")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.clearFocus()
                }
            }
        }
        container.addView(webView)

        mainHandler.postDelayed({
            webView?.loadUrl("file:///android_asset/index.html")
        }, 30)

        return container
    }

    // Генератор нативной сетки клавиш (Адаптивный под планшеты)
    private fun renderNativeKeys(context: ContextThemeWrapper) {
        nativeKeyboardLayout.removeAllViews()
        val density = resources.displayMetrics.density
        
        // Определяем планшет по ширине экрана (если dp >= 600, то это планшет)
        val isTablet = (resources.configuration.screenWidthDp >= 600)
        
        // Адаптивные отступы: больше на планшетах для удобства разделения клавиш
        val marginPx = if (isTablet) (4 * density).toInt() else (2 * density).toInt()

        val currentLayoutData = layouts[currentLanguage] ?: return

        for (rowData in currentLayoutData) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }

            for (key in rowData) {
                val btn = Button(context).apply {
                    // Обработка регистра (Shift) для букв
                    text = if (isShiftActive && key.length == 1 && key[0].isLetter()) {
                        key.uppercase()
                    } else {
                        key
                    }

                    // Адаптивный вес (ширина) кнопок
                    val weight = when (key) {
                        "Пробел" -> 4f
                        "Enter", "⇧", "⌫" -> 1.5f
                        else -> 1f
                    }

                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                        setMargins(marginPx, marginPx, marginPx, marginPx)
                    }

                    // Стилизация кнопок под Material Dark
                    if (key == "⇧" && isShiftActive) {
                        setBackgroundColor(android.graphics.Color.parseColor("#444444")) // Активный Shift
                    } else if (listOf("⇧", "⌫", "Enter", "🌐").contains(key)) {
                        setBackgroundColor(android.graphics.Color.parseColor("#1e1e1e")) // Системные кнопки
                    } else {
                        setBackgroundColor(android.graphics.Color.parseColor("#2d2d2d")) // Обычные буквы
                    }
                    
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = if (isTablet) 20f else 16f // Крупный шрифт на планшетах
                    isAllCaps = false // Отключаем автоматический верхний регистр Android

                    // Логика кликов нативной части
                    setOnClickListener {
                        when (key) {
                            "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
                            "Enter" -> {
                                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, 66))
                                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, 66))
                            }
                            "Пробел" -> currentInputConnection?.commitText(" ", 1)
                            "⇧" -> {
                                isShiftActive = !isShiftActive
                                renderNativeKeys(context) // Перерисовываем буквы в новом регистре
                            }
                            "🌐" -> {
                                // Переключение языка по кругу
                                currentLanguage = if (currentLanguage == "RU") "EN" else "RU"
                                // Сообщаем в WebView, чтобы обновить индикатор на верхней панели
                                webView?.evaluateJavascript("updateLangIndicator('$currentLanguage');", null)
                                renderNativeKeys(context)
                            }
                            else -> {
                                val textToCommit = if (isShiftActive) key.uppercase() else key
                                currentInputConnection?.commitText(textToCommit, 1)
                                
                                // Сброс Shift после одной буквы
                                if (isShiftActive) {
                                    isShiftActive = false
                                    renderNativeKeys(context)
                                }
                            }
                        }
                    }
                }
                row.addView(btn)
            }
            nativeKeyboardLayout.addView(row)
        }
    }

    fun setWidgetOverlayState(expanded: Boolean) {
        mainHandler.post {
            val params = webView?.layoutParams as? FrameLayout.LayoutParams
            if (params != null) {
                val density = resources.displayMetrics.density
                if (expanded) {
                    // Виджеты плавно раскрываются на полные 280dp высоты
                    params.height = FrameLayout.LayoutParams.MATCH_PARENT
                } else {
                    params.height = (topBarHeightDp * density).toInt()
                }
                webView?.layoutParams = params
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        webView?.destroy()
        super.onDestroy()
    }

    class WebAppInterface(private val service: MyKeyboardService) {
        @JavascriptInterface
        fun commitText(text: String) {
            Handler(Looper.getMainLooper()).post { service.currentInputConnection?.commitText(text, 1) }
        }

        @JavascriptInterface
        fun toggleWidgetView(expand: Boolean) {
            service.setWidgetOverlayState(expand)
        }
    }
}
