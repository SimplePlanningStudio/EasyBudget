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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidisland.ezpermission.EzPermission
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentMonthlyReportBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.report.adapter.MainAdapter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import androidx.core.net.toUri
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.extensions.openPDfReport
import com.simplebudget.iab.isUserPremium


/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportFragment : BaseFragment<FragmentMonthlyReportBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: LocalDate

    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val viewModel: MonthlyReportViewModel by viewModel()
    private val pdfViewModel: PDFViewModel by viewModel()
    private val toastManager: ToastManager by inject()
    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading = false
    private var monthlyReportData: DataModels.MonthlyReportData.Data? = null
    private val expensesList: ArrayList<DataModels.SuperParent> = ArrayList()

    private val storagePermissions = arrayOf(
        WRITE_EXTERNAL_STORAGE,
        READ_EXTERNAL_STORAGE
    )

    lateinit var mainAdapter: MainAdapter
    private var adView: AdView? = null
// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentMonthlyReportBinding =
        FragmentMonthlyReportBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getSerializable(ARG_DATE, LocalDate::class.java) as LocalDate
        } else {
            requireArguments().getSerializable(ARG_DATE) as LocalDate
        }

        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentProgressBar?.visibility = View.GONE
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE
            if (result == null)
                emptyViews()
            result?.let {
                when (result) {
                    DataModels.MonthlyReportData.Empty -> {
                        emptyViews()
                    }

                    is DataModels.MonthlyReportData.Data -> {
                        monthlyReportData = result
                        expensesList.clear()
                        expensesList.addAll(result.allExpensesParentList)

                        if (expensesList.isEmpty()) {
                            emptyViews()
                            return@observe
                        }

                        mainAdapter = MainAdapter(
                            expensesList,
                            appPreferences,
                            onBannerClick = { banner ->
                                /* val bundle = Bundle().apply {
                                     putString("banner_name", banner.app_name)
                                     putString("app_package", banner.package_name)
                                 }
                                 logAnalyticEvent("banner_clicked", bundle)*/
                                if (banner.redirectUrl != null) {
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, banner.redirectUrl.toUri())
                                    startActivity(intent)
                                }
                            }
                        )
                        configureRecyclerView(
                            binding?.monthlyReportFragmentRecyclerView!!,
                            mainAdapter
                        )

                        binding?.monthlyReportFragmentRevenuesTotalTv?.text =
                            CurrencyHelper.getFormattedCurrencyString(
                                appPreferences,
                                result.revenuesAmount
                            )
                        binding?.monthlyReportFragmentExpensesTotalTv?.text =
                            CurrencyHelper.getFormattedCurrencyString(
                                appPreferences,
                                result.expensesAmount
                            )

                        val balance = result.revenuesAmount - result.expensesAmount
                        binding?.monthlyReportFragmentBalanceTv?.text =
                            CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
                        binding?.monthlyReportFragmentBalanceTv?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (balance >= 0) R.color.budget_green else R.color.budget_red
                            )
                        )
                    }
                }
            }

        }

        viewModel.loadDataForMonth(date)

        /**
         * Export for this month
         */
        binding?.relativeLayoutExport?.setOnClickListener {
            if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
                exportSelectionDialog()
            } else {
                showInterstitial()
            }
        }

        /*
          Observe export status
         */
        pdfViewModel.observeExportStatus.observe(viewLifecycleOwner) { result ->
            //Get data list update it to UI, notify scroll down
            result?.let {
                if (it.message.isNotEmpty())
                    toastManager.showShort(it.message)

                if (!it.status) return@let

                it.file?.let { file ->
                    shareCsvFile(file)
                }
            }
        }
        /*
          Observe HTML/PDF report generation
         */
        pdfViewModel.observeGeneratePDFReport.observe(viewLifecycleOwner) { uri ->
            requireActivity().openPDfReport(uri, expensesList.isEmpty())
        }
        /**
         * Banner ads
         */
        binding?.adViewContainer?.let {
            loadBanner(
                appPreferences.isUserPremium(),
                binding?.adViewContainer!!,
                onBannerAdRequested = { bannerAdView ->
                    this.adView = bannerAdView
                }
            )
        }
    }

    /**
     * When there's no data
     */
    private fun emptyViews() {
        binding?.monthlyReportFragmentRecyclerView?.visibility = View.GONE
        binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE

        binding?.monthlyReportFragmentRevenuesTotalTv?.text =
            CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
        binding?.monthlyReportFragmentExpensesTotalTv?.text =
            CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
        binding?.monthlyReportFragmentBalanceTv?.text =
            CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
        binding?.monthlyReportFragmentBalanceTv?.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.budget_green
            )
        )
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        /*adapter: MonthlyReportRecyclerViewAdapter*/
        adapter: MainAdapter,
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: LocalDate): MonthlyReportFragment = MonthlyReportFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     * Called when opening the activity
     */
    override fun onResume() {
        resumeBanner(adView)
        super.onResume()
    }

    override fun onDestroyView() {
        destroyBanner(adView)
        adView = null
        super.onDestroyView()
    }

    /**
     * show Interstitial
     */
    private fun showInterstitial() {
        binding?.frameLayoutOpaque?.visibility = View.VISIBLE
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
        if (InternetUtils.isInternetAvailable(requireActivity()).not()) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            requireActivity(),
            getString(R.string.interstitial_ad_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    binding?.frameLayoutOpaque?.visibility = View.GONE
                    mInterstitialAd = null
                    mAdIsLoading = false
                    exportSelectionDialog()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // Ad was loaded
                    binding?.frameLayoutOpaque?.visibility = View.GONE
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
                binding?.frameLayoutOpaque?.visibility = View.GONE
                exportSelectionDialog()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                binding?.frameLayoutOpaque?.visibility = View.GONE
                exportSelectionDialog()
            }

            override fun onAdShowedFullScreenContent() {
                binding?.frameLayoutOpaque?.visibility = View.GONE
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
            "Open or share pdf",
            "Share spreadsheet"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                String.format(
                    getString(R.string.budget_report_of),
                    date.formatLocalDate("MMM yyyy")
                )
            )
            .setItems(weeks) { dialog, which ->
                dialog.dismiss()
                if (monthlyReportData != null) {
                    if (which == 0) {
                        pdfViewModel.generatePdfReport(requireActivity(), date, monthlyReportData!!)
                        //Log event
                        analyticsManager.logEvent(Events.KEY_PRINT_PDF_REPORT)

                    } else {
                        exportExcel()
                        //Log event
                        analyticsManager.logEvent(Events.KEY_PRINT_EXCEL_REPORT)
                    }
                } else {
                    if (expensesList.isEmpty()) {
                        toastManager.showLong(getString(R.string.no_expenses_found))
                    } else {

                        DialogUtil.errorDialog(
                            requireContext(),
                            getString(R.string.error_report_generation)
                        )?.show()
                    }
                }
            }
            .setPositiveButton(getString(R.string.cancel)) { dialog, p1 ->
                dialog.dismiss()
            }.setCancelable(false)
            .show()
        //Log event
        analyticsManager.logEvent(Events.KEY_REPORT_EXPORT)
    }

    /**
     * Check / Ask Storage permission and
     * Export data into CSV file
     */
    private fun exportExcel() {
        when {
            isStoragePermissionsGranted() -> {
                val fileNameDateFormat = DateTimeFormatter.ofPattern(
                    "MMMyyyy",
                    Locale.getDefault()
                )
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
                pdfViewModel.exportCSV(
                    appPreferences.getUserCurrency(),
                    date,
                    file,
                    monthlyReportData!!
                )
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
        try {
            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                BuildConfig.APPLICATION_ID + ".fileprovider", file
            )
            intentShareCSV(requireActivity(), fileUri)
        } catch (_: Exception) {
            DialogUtil.errorDialog(
                context = requireActivity(),
                message = getString(R.string.error_sharing_report)
            )
        }
    }
}
