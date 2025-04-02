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
package com.simplebudget.view.report

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.simplebudget.databinding.ActivityPdfReportBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.MultiClick

/**
 * Activity that displays monthly report
 *
 * @author Waheed Nazir
 */
class PDFReportActivity : BaseActivity<ActivityPdfReportBinding>() {


    override fun createBinding(): ActivityPdfReportBinding {
        return ActivityPdfReportBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent?.extras?.getString(INTENT_CODE_PDF_CONTENTS)?.let { htmlContents ->
            loadContents(htmlContents)
        }

        binding.btnDownloadPrint.setOnClickListener {
            MultiClick.avoid(it)
            createWebPrintJob(binding.webView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadContents(htmlContents: String) {
        binding.webView.clearCache(true)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.setSupportZoom(true)
        binding.webView.settings.builtInZoomControls = false
        binding.webView.settings.displayZoomControls = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.isVerticalScrollBarEnabled = true
        binding.webView.isHorizontalScrollBarEnabled = true
        binding.webView.isScrollbarFadingEnabled = false
        binding.webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        binding.webView.isScrollbarFadingEnabled = false
        binding.webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                binding.progressBar.progress = progress
                Log.d("PROGRESS: ", progress.toString())
                if (progress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
        binding.webView.loadDataWithBaseURL(null, htmlContents, "text/html", "utf-8", null)

        createWebPrintJob(binding.webView)
    }

    private fun createWebPrintJob(webView: WebView) {
        // Get a PrintManager instance
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        // Get a print adapter instance
        val printAdapter = webView.createPrintDocumentAdapter()
        // Create a print job with name and adapter instance
        val jobName = "Budget Report"
        printManager.print(
            jobName, printAdapter,
            PrintAttributes.Builder().build()
        )
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

    /**
     *
     */
    companion object {
        const val INTENT_CODE_PDF_CONTENTS = "htmlPdfContents"
    }
}
