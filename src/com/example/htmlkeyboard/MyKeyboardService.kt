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

    override fun onCreateInputView(): View {
        val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)

        // Главный контейнер клавиатуры (Фиксированная высота 250dp)
        container = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (250 * resources.displayMetrics.density).toInt()
            )
        }

        // 1. НАЦИОНАЛЬНАЯ СИСТЕМНАЯ РЕАЛИЗАЦИЯ КЛАВИШ (LinearLayout)
        val nativeKeyboardLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { 
                // Оставляем сверху 40dp под полоску веб-виджетов
                topMargin = (40 * resources.displayMetrics.density).toInt() 
            }
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#121212"))
        }

        // Пример генерации одного нативного ряда (Й Ц У К Е Н) для теста системы
        val row1 = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val testChars = listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ")
        for (char in testChars) {
            val btn = Button(themedContext).apply {
                text = char
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(2, 2, 2, 2)
                }
                setBackgroundColor(android.graphics.Color.parseColor("#2d2d2d"))
                setTextColor(android.graphics.Color.WHITE)
                setOnClickListener { currentInputConnection?.commitText(char, 1) }
            }
            row1.addView(btn)
        }
        nativeKeyboardLayout.addView(row1)
        container.addView(nativeKeyboardLayout)

        // 2. ВЕБ-ИНТЕРФЕЙС ДЛЯ ВАШИХ ВИДЖЕТОВ
        webView = WebView(themedContext).apply {
            // Изначально WebView свернут в полоску высотой 40dp сверху
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (40 * resources.displayMetrics.density).toInt()
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

        // Загрузка вашего index.html
        mainHandler.postDelayed({
            webView?.loadUrl("file:///android_asset/index.html")
        }, 30)

        return container
    }

    // Метод изменения размера WebView: раскрытие оверлея на весь экран клавиатуры
    fun setWidgetOverlayState(expanded: Boolean) {
        mainHandler.post {
            val params = webView?.layoutParams as? FrameLayout.LayoutParams
            if (params != null) {
                if (expanded) {
                    // Раскрываем веб-виджеты на всю высоту (250dp), перекрывая системные кнопки
                    params.height = FrameLayout.LayoutParams.MATCH_PARENT
                } else {
                    // Сворачиваем обратно в верхнюю полоску (40dp)
                    params.height = (40 * resources.displayMetrics.density).toInt()
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

        // JS вызывает этот метод, когда пользователь открывает/закрывает панель виджетов
        @JavascriptInterface
        fun toggleWidgetView(expand: Boolean) {
            service.setWidgetOverlayState(expand)
        }
    }
}
