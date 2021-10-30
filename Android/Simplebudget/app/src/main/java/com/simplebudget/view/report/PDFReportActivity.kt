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
import com.simplebudget.R
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.MultiClick
import kotlinx.android.synthetic.main.activity_pdf_report.*


/**
 * Activity that displays monthly report
 *
 * @author Waheed Nazir
 */
class PDFReportActivity : BaseActivity() {

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_report)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent?.extras?.getString(INTENT_CODE_PDF_CONTENTS)?.let { htmlContents ->
            loadContents(htmlContents)
        }

        btnDownloadPrint.setOnClickListener {
            MultiClick.avoid(it)
            createWebPrintJob(webView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadContents(htmlContents: String) {
        webView.clearCache(true)
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = false
        webView.settings.displayZoomControls = true
        webView.settings.loadWithOverviewMode = true
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.isScrollbarFadingEnabled = false
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = false
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                progressBar.progress = progress
                Log.d("PROGRESS: ", progress.toString())
                if (progress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContents, "text/html", "utf-8", null)

        createWebPrintJob(webView)
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
