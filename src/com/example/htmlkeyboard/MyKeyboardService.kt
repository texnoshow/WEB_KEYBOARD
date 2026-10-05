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
        // 1. ЗАЩИТА: Оборачиваем контекст сервиса в ContextThemeWrapper, 
        // чтобы у WebView была стандартная тема для отрисовки окон.
        val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)

        val container = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                // Используем фиксированную высоту в dp (например, 250dp переводятся в px),
                // так как notification_large_icon_width часто возвращает некорректный размер для IME.
                (250 * resources.displayMetrics.density).toInt()
            )
        }

        // 2. Инициализируем WebView с безопасным контекстом
        webView = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // ЗАЩИТА: Отключаем аппаратное ускорение ТОЛЬКО для WebView, 
            // если краш происходит на уровне RenderThread внутри сервиса
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            addJavascriptInterface(WebAppInterface(this@MyKeyboardService), "AndroidKeyboard")
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/index.html")
        }

        container.addView(webView)
        return container
    }

    // 3. ЗАЩИТА: Очищаем ресурсы при закрытии клавиатуры, чтобы избежать утечек памяти и крашей
    override fun onDestroyInputView() {
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        super.onDestroyInputView()
    }

    class WebAppInterface(private val service: InputMethodService) {
        private val mainHandler = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun commitText(text: String) {
            // Вызов строго в главном потоке
            mainHandler.post {
                val inputConnection = service.currentInputConnection
                inputConnection?.commitText(text, 1)
            }
        }
    }
}
