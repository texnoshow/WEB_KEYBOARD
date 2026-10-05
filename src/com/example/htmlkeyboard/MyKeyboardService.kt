package com.example.htmlkeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MyKeyboardService : InputMethodService() {

    private lateinit var webView: WebView

    override fun onCreateInputView(): View {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                // Используем фиксированную высоту или более подходящий ресурс, 
                // так как notification_large_icon_width не предназначен для высоты клавиатуры
                resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) * 6
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            addJavascriptInterface(WebAppInterface(this@MyKeyboardService), "AndroidKeyboard")
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/index.html")
        }

        container.addView(webView)
        return container
    }

    class WebAppInterface(private val service: InputMethodService) {
        // Создаем Handler для перенаправления вызовов в главный поток
        private val mainHandler = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun commitText(text: String) {
            mainHandler.post {
                val inputConnection = service.currentInputConnection
                inputConnection?.commitText(text, 1)
            }
        }
    }
}
