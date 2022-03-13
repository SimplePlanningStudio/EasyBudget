/*
 *   Copyright 2021 Benoit LETONDOR
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

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidisland.ezpermission.EzPermission
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.readData.utils.intentShareCSV
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.getUserCurrency
import com.simplebudget.helper.toast
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import kotlinx.android.synthetic.main.fragment_monthly_report.view.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val ARG_DATE = "arg_date"

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportFragment : Fragment() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: Date

    private val appPreferences: AppPreferences by inject()
    private val viewModel: MonthlyReportViewModel by viewModel()
    private lateinit var thisView: View
    private var adView: AdView? = null

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading = false

    private val storagePermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

// ---------------------------------->

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        date = requireArguments().getSerializable(ARG_DATE) as Date

        // Inflate the layout for this fragment
        thisView = inflater.inflate(R.layout.fragment_monthly_report, container, false)

        val progressBar =
            thisView.findViewById<ProgressBar>(R.id.monthly_report_fragment_progress_bar)
        val content = thisView.findViewById<View>(R.id.monthly_report_fragment_content)
        val recyclerView =
            thisView.findViewById<RecyclerView>(R.id.monthly_report_fragment_recycler_view)
        val emptyState = thisView.findViewById<View>(R.id.monthly_report_fragment_empty_state)
        val revenuesAmountTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_revenues_total_tv)
        val expensesAmountTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_expenses_total_tv)
        val balanceTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_balance_tv)

        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            progressBar.visibility = View.GONE
            content.visibility = View.VISIBLE

            when (result) {
                MonthlyReportViewModel.MonthlyReportData.Empty -> {
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE

                    revenuesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    expensesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    balanceTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    balanceTextView.setTextColor(
                        ContextCompat.getColor(
                            balanceTextView.context,
                            R.color.budget_green
                        )
                    )
                }
                is MonthlyReportViewModel.MonthlyReportData.Data -> {
                    configureRecyclerView(
                        recyclerView,
                        MonthlyReportRecyclerViewAdapter(
                            result.expenses,
                            result.revenues,
                            result.allExpensesOfThisMonth,
                            appPreferences
                        )
                    )

                    revenuesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences,
                            result.revenuesAmount
                        )
                    expensesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences,
                            result.expensesAmount
                        )

                    val balance = result.revenuesAmount - result.expensesAmount
                    balanceTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
                    balanceTextView.setTextColor(
                        ContextCompat.getColor(
                            balanceTextView.context,
                            if (balance >= 0) R.color.budget_green else R.color.budget_red
                        )
                    )
                }
            }
        }

        viewModel.loadDataForMonth(date)

        /**
         * Export for this month
         */
        thisView.ic_export.setOnClickListener {
            if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
                exportSelectionDialog()
            } else {
                showInterstitial()
            }
        }

        /*
          Observe export status
         */
        viewModel.observeExportStatus.observe(viewLifecycleOwner) { result ->
            //Get data list update it to UI, notify scroll down
            result?.let {
                if (it.message.isNotEmpty())
                    requireActivity().toast(it.message)

                if (!it.status) return@let

                it.file?.let { file ->
                    shareCsvFile(file)
                }
            }
        }
        /*
          Observe HTML/PDF report generation
         */
        viewModel.observeGeneratePDFReport.observe(viewLifecycleOwner) { htmlReport ->
            htmlReport?.let {
                if (htmlReport.html.isEmpty() || htmlReport.isEmpty) {
                    requireActivity().toast("Please add expenses of this month to generate report.")
                } else {
                    startActivity(
                        Intent(requireActivity(), PDFReportActivity::class.java)
                            .putExtra(PDFReportActivity.INTENT_CODE_PDF_CONTENTS, htmlReport.html)
                    )
                }
            }
        }

        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            val adContainerView = thisView.findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

        return thisView
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        adapter: MonthlyReportRecyclerViewAdapter
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: Date): MonthlyReportFragment = MonthlyReportFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
    }

    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            val adContainerView = thisView.findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                requireContext(),
                requireActivity().windowManager.defaultDisplay
            )!!
            adView = AdView(requireContext())
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            adContainerView.addView(adView)
            val actualAdRequest = AdRequest.Builder()
                .build()
            adView?.adSize = adSize
            adView?.loadAd(actualAdRequest)
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
                override fun onAdClosed() {
                    loadAndDisplayBannerAds()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    /**
     * Called when opening the activity
     */
    override fun onResume() {
        adView?.resume()
        super.onResume()
    }


    /**
     * show Interstitial
     */
    private fun showInterstitial() {
        thisView.frameLayoutOpaque.visibility = View.VISIBLE
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(requireActivity())
        } else {
            // The interstitial ad wasn't ready yet
            loadInterstitial()
        }
    }

    /**
     * Load Interstitial
     */
    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            requireActivity(),
            getString(R.string.interstitial_ad_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    thisView.frameLayoutOpaque.visibility = View.GONE
                    mInterstitialAd = null
                    mAdIsLoading = false
                    exportSelectionDialog()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // Ad was loaded
                    thisView.frameLayoutOpaque.visibility = View.GONE
                    mInterstitialAd = interstitialAd
                    mAdIsLoading = false
                    listenInterstitialAds()
                    showInterstitial()
                }
            })
    }

    /**
     *
     */
    private fun listenInterstitialAds() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                thisView.frameLayoutOpaque.visibility = View.GONE
                exportSelectionDialog()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                thisView.frameLayoutOpaque.visibility = View.GONE
                exportSelectionDialog()
            }

            override fun onAdShowedFullScreenContent() {
                thisView.frameLayoutOpaque.visibility = View.GONE
                // Ad showed fullscreen content
                mInterstitialAd = null
            }
        }
    }


    /**
     * Check if the app can writes on the shared storage
     *
     *  On Android 10 (API 29), we can add files to the Downloads folder without having to request the
     * [WRITE_EXTERNAL_STORAGE] permission, so we only check on pre-API 29 devices
     */
    private fun isStoragePermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            (EzPermission.isGranted(requireContext(), storagePermissions[0])
                    && EzPermission.isGranted(requireContext(), storagePermissions[1]))
        }
    }

    /**
     *
     */
    private fun askStoragePermission() {
        EzPermission
            .with(requireContext())
            .permissions(storagePermissions[0], storagePermissions[1])
            .request { granted, denied, permanentlyDenied ->
                if (granted.contains(storagePermissions[0]) &&
                    granted.contains(storagePermissions[1])
                ) { // Storage permissions already Granted
                    exportSelectionDialog()
                } else if (denied.contains(storagePermissions[0]) ||
                    denied.contains(storagePermissions[1])
                ) { // Denied
                    showStorageDeniedDialog()
                } else if (permanentlyDenied.contains(storagePermissions[0]) ||
                    permanentlyDenied.contains(storagePermissions[1])
                ) { // Storage Permanently denied
                    showStoragePermanentlyDeniedDialog()
                }

            }
    }

    /**
     *
     */
    private fun showStoragePermanentlyDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(getString(R.string.title_storage_permission_permanently_denied))
        dialog.setMessage(getString(R.string.message_storage_permission_permanently_denied))
        dialog.setNegativeButton(getString(R.string.not_now)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.settings)) { _, _ ->
            startActivity(
                EzPermission.appDetailSettingsIntent(
                    requireContext()
                )
            )
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }


    /**
     *
     */
    private fun showStorageDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(getString(R.string.title_storage_permission_denied))
        dialog.setMessage(getString(R.string.message_storage_permission_denied))
        dialog.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.allow)) { _, _ ->
            askStoragePermission()
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }

    /**
     * Dialog to show report export options
     */
    private fun exportSelectionDialog() {
        val weeks = arrayOf(
            "Download or print pdf",
            "Share spreadsheet"
        )
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(String.format("Budget report of %s", monthFormat.format(date)))
            .setItems(weeks) { dialog, which ->
                if (which == 0) {
                    viewModel.generateHtml(appPreferences.getUserCurrency(), date)
                } else {
                    exportExcel()
                }
                dialog.dismiss()
            }
            .setPositiveButton("Not now") { dialog, p1 ->
                dialog.dismiss()
            }.setCancelable(false)
            .show()
    }

    /**
     * Check / Ask Storage permission and
     * Export data into CSV file
     */
    private fun exportExcel() {
        when {
            isStoragePermissionsGranted() -> {
                val fileNameDateFormat = SimpleDateFormat("MMMyyyy", Locale.getDefault())
                val file: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val exportDir = getAppSpecificDocumentStorageDirAboveAndEqualToAPI29()
                    if (!exportDir.isDirectory) exportDir.mkdirs()
                    File(
                        exportDir,
                        "${String.format("%s", fileNameDateFormat.format(date))}_SimpleBudget.csv"
                    )
                } else {
                    val exportDir = getAppSpecificDocumentStorageDirBelowAndEqualToAPI28()
                    if (!exportDir.isDirectory) exportDir.mkdirs()
                    File(
                        exportDir,
                        "${String.format("%s", fileNameDateFormat.format(date))}_SimpleBudget.csv"
                    )
                }
                viewModel.exportCSV(appPreferences.getUserCurrency(), date, file)
            }
            else -> {
                askStoragePermission()
            }
        }
    }

    /**
     * Above and equal to 29
     */
    private fun getAppSpecificDocumentStorageDirAboveAndEqualToAPI29(): File {
        // Get the documents directory that's inside the app-specific directory on external storage.
        val exportDir =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports")
        if (!exportDir.mkdirs()) {
            Log.e("LOG_TAG", "Directory not created")
        }
        return exportDir
    }

    /**
     * Below and equal to 28
     */
    private fun getAppSpecificDocumentStorageDirBelowAndEqualToAPI28(): File {
        // Get the documents directory that's inside the app-specific directory on external storage.
        val exportDir = File(Environment.getExternalStorageDirectory().absolutePath)
        if (!exportDir.isDirectory) exportDir.mkdirs()

        return exportDir
    }

    /**
     *
     */
    private fun shareCsvFile(file: File) {
        val fileUri = FileProvider.getUriForFile(
            requireContext(),
            BuildConfig.APPLICATION_ID + ".fileprovider", file
        )
        intentShareCSV(requireActivity(), fileUri)

        /*val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setTitle("Share Spreadsheet")
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }.setPositiveButton("Yes") { dialog, _ ->
                intentShareCSV(requireActivity(), fileUri)
                dialog.cancel()
            }
        builder.create().show()*/
    }
}
