package com.example.htmlkeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MyKeyboardService : InputMethodService() {

    private var webView: WebView? = null

    override fun onCreateInputView(): View {
        // Используем контекст темы устройства
        val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)

        // Контейнер фиксированной высоты (250dp)
        val container = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (250 * resources.displayMetrics.density).toInt()
            )
        }

        webView = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // КРИТИЧЕСКИ ВАЖНО ДЛЯ КЛАВИАТУР:
            // Отключаем фокус для WebView, чтобы фокус оставался в активном приложении (блокнот, мессенджер)
            isFocusable = false
            isFocusableInTouchMode = false
            clearFocus()

            // Настройки WebSettings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true // Позволяет корректно читать файлы из assets
            
            // Связываем JavaScript с Kotlin-интерфейсом
            addJavascriptInterface(WebAppInterface(this@MyKeyboardService), "AndroidKeyboard")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Дополнительная страховка фокуса после полной загрузки страницы
                    view?.clearFocus()
                }
            }
            
            loadUrl("file:///android_asset/index.html")
        }

        container.addView(webView)
        return container
    }

    // Вызывается, когда пользователь переключает фокус на другое текстовое поле
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Страхуемся, чтобы при открытии клавиатуры WebView гарантированно не забирал фокус
        webView?.clearFocus()
    }

    // Безопасная очистка WebView при уничтожении службы клавиатуры
    override fun onDestroy() {
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    // Интерфейс для взаимодействия JavaScript -> Kotlin
    class WebAppInterface(private val service: MyKeyboardService) {
        private val mainHandler = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun commitText(text: String) {
            mainHandler.post {
                // Извлекаем актуальное соединение ввода в основном потоке
                val inputConnection = service.currentInputConnection
                if (inputConnection != null) {
                    inputConnection.commitText(text, 1)
                }
            }
        }
    }
}
