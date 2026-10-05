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
        val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)

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
            mainHandler.post {
                val inputConnection = service.currentInputConnection
                inputConnection?.commitText(text, 1)
            }
        }
    }
}
