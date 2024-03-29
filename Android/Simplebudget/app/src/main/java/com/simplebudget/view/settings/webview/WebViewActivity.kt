package com.simplebudget.view.settings.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityWebViewBinding
import com.simplebudget.helper.DialogUtil
import com.simplebudget.helper.Rate

class WebViewActivity : BaseActivity<ActivityWebViewBinding>() {

    /**
     *
     */
    override fun createBinding(): ActivityWebViewBinding =
        ActivityWebViewBinding.inflate(layoutInflater)


    companion object {
        private const val REQUEST_URL = "URL"
        private const val REQUEST_SCREEN_TITLE = "ScreenTitle"
        fun start(context: Context, url: String? = null, screenTitle: String? = null) {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(REQUEST_URL, url)
            intent.putExtra(REQUEST_SCREEN_TITLE, screenTitle)
            context.startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.cacheMode = LOAD_NO_CACHE

        // I'll handle javascript click in case I'm on reviews page
        if ((intent.getStringExtra(REQUEST_URL)
                ?: "") == getString(R.string.simple_budget_reviews_url)
        ) {
            val jsInterface = JavaScriptInterface(this)
            binding.webView.addJavascriptInterface(jsInterface, "Android")
            binding.shareYourReview.visibility = View.VISIBLE
            binding.shareYourReview.setOnClickListener {
                Rate.onPlayStore(this)
            }
        }
        // Set a WebViewClient to handle page navigation within the WebView
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(
                view: WebView?, url: String?, favicon: android.graphics.Bitmap?
            ) {
                binding.progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?, errorCode: Int, description: String?, failingUrl: String?
            ) {
                binding.progress.visibility = View.GONE
                DialogUtil.createDialog(this@WebViewActivity,
                    title = "Error",
                    message = description ?: "Something went wrong, please try again. Thank you",
                    positiveBtn = getString(R.string.ok),
                    negativeBtn = "",
                    isCancelable = true,
                    positiveClickListener = {
                        finish()
                    },
                    negativeClickListener = {})
                    .show()
            }
        }

        // Load a web page into the WebView
        val simpleBudgetWebUrl = getString(R.string.simple_budget_web_url)
        if (intent.hasExtra(REQUEST_URL) && intent.getStringExtra(REQUEST_URL) != null) {
            binding.webView.loadUrl(intent.getStringExtra(REQUEST_URL)!!)
        } else {
            binding.webView.loadUrl(simpleBudgetWebUrl)
        }

        if (intent.hasExtra(REQUEST_SCREEN_TITLE)) {
            intent.getStringExtra(REQUEST_SCREEN_TITLE)?.let {
                title = intent.getStringExtra(REQUEST_SCREEN_TITLE)
            }
        }
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    class JavaScriptInterface(private val context: Context) {
        @JavascriptInterface
        fun handleReviewClick() {
            Rate.onPlayStore(context)
        }
    }
}