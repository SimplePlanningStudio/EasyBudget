/*
 *   Copyright 2025 Waheed Nazir
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.simplebudget.view.settings.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityWebViewBinding
import com.simplebudget.helper.DialogUtil
import com.simplebudget.helper.Rate
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import org.koin.android.ext.android.inject
import androidx.core.net.toUri

class WebViewActivity : BaseActivity<ActivityWebViewBinding>() {

    private val analyticsManager: AnalyticsManager by inject()

    /**
     *
     */
    override fun createBinding(): ActivityWebViewBinding =
        ActivityWebViewBinding.inflate(layoutInflater)


    companion object {
        private const val REQUEST_URL = "URL"
        private const val REQUEST_SCREEN_TITLE = "ScreenTitle"
        private const val REQUEST_ENABLE_SHARE_REVIEW_BUTTON = "shareYourReviewButton"
        fun start(
            context: Context,
            url: String? = null,
            screenTitle: String? = null,
            enableShareReview: Boolean = true,
        ) {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(REQUEST_URL, url)
            intent.putExtra(REQUEST_SCREEN_TITLE, screenTitle)
            intent.putExtra(REQUEST_ENABLE_SHARE_REVIEW_BUTTON, enableShareReview)
            context.startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        binding.webView.settings.apply {
            javaScriptEnabled = true
            cacheMode = LOAD_NO_CACHE
            // This will make sure text size stays consistent regardless of system font scale
            textZoom = 100
        }

        // I'll handle javascript click in case I'm on reviews page
        if ((intent.getStringExtra(REQUEST_URL)
                ?: "") == getString(R.string.simple_budget_reviews_url)
        ) {
            val jsInterface = JavaScriptInterface(this)
            binding.webView.addJavascriptInterface(jsInterface, "Android")
            binding.shareYourReview.visibility = if (intent.getBooleanExtra(
                    REQUEST_ENABLE_SHARE_REVIEW_BUTTON, true
                )
            ) View.VISIBLE else View.GONE
            binding.shareYourReview.setOnClickListener {
                analyticsManager.logEvent(Events.KEY_REVIEW_APP_RATE)
                Rate.onPlayStore(this)
            }
        }
        // Set a WebViewClient to handle page navigation within the WebView
        binding.webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?, url: String?, favicon: android.graphics.Bitmap?,
            ) {
                binding.progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?, errorCode: Int, description: String?, failingUrl: String?,
            ) {
                binding.progress.visibility = View.GONE
                if (this@WebViewActivity.isFinishing.not() && this@WebViewActivity.isDestroyed.not()) {
                    DialogUtil.createDialog(
                        this@WebViewActivity,
                        title = "Error",
                        message = description
                            ?: "Something went wrong, please try again. Thank you",
                        positiveBtn = getString(R.string.ok),
                        negativeBtn = "",
                        isCancelable = true,
                        positiveClickListener = {
                            finish()
                        },
                        negativeClickListener = {})?.show()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()

                return when {
                    url.startsWith("mailto:") -> {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = url.toUri()
                        }
                        try {
                            view?.context?.startActivity(emailIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(view?.context, "No email app found", Toast.LENGTH_SHORT)
                                .show()
                        }
                        true
                    }

                    url.startsWith("tel:") -> {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = url.toUri()
                        }
                        view?.context?.startActivity(dialIntent)
                        true
                    }

                    else -> false // Let WebView load the URL normally
                }
            }
        }

        // Load a web page into the WebView
        val simpleBudgetWebUrl = getString(R.string.simple_budget_web_url)
        if (intent.hasExtra(REQUEST_URL) && intent.getStringExtra(REQUEST_URL) != null) {
            binding.webView.loadUrl(intent.getStringExtra(REQUEST_URL)!!)
        } else {
            binding.webView.loadUrl(simpleBudgetWebUrl)
        }

        val screenTitle: String? = intent.getStringExtra(REQUEST_SCREEN_TITLE)
        if (screenTitle != null) {
            title = screenTitle
            //Screen name event
            val eventName = screenTitle.replace(" ", "_").lowercase().plus("_screen")
            analyticsManager.logEvent(eventName)
        } else {
            //Screen name event
            analyticsManager.logEvent(Events.KEY_USER_TESTIMONIAL_SCREEN)
        }

        // Add custom back handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleWebViewBackPress()
            }
        })
    }

    private fun handleWebViewBackPress() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }


    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            handleWebViewBackPress()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class JavaScriptInterface(private val context: Context) {
        @JavascriptInterface
        fun handleReviewClick() {
            Rate.onPlayStore(context)
        }
    }
}