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
package com.simplebudget.view.search

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidisland.ezpermission.EzPermission
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentSearchBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.model.account.appendAccount
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.view.report.DataModels
import com.simplebudget.view.report.MonthlyReportFragment
import com.simplebudget.view.report.PDFReportActivity
import com.simplebudget.view.report.adapter.MainAdapter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri


class SearchFragment : BaseFragment<FragmentSearchBinding>() {

    private lateinit var date: LocalDate
    private val appPreferences: AppPreferences by inject()
    private val viewModel: SearchViewModel by viewModel()
    private val toastManager: ToastManager by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private var adView: AdView? = null
    private val dayFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.getDefault())

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading = false

    private val storagePermissions = arrayOf(
        WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    )
// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentSearchBinding = FragmentSearchBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        date = LocalDate.now() // Or we can get from args if we plan to handle it with notification

        //Log event
        analyticsManager.logEvent(
            Events.KEY_SEARCH_FILTER, mapOf(
                Events.KEY_VALUE to SearchUtil.THIS_MONTH
            )
        )

        viewModel.allExpenses.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE
            if (result.isEmpty()) {
                binding?.exportReport?.visibility = View.GONE
                binding?.totalSearchRecords?.visibility = View.GONE
                binding?.recyclerViewSearch?.visibility = View.GONE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE
            } else {
                binding?.exportReport?.visibility = View.VISIBLE
                binding?.totalSearchRecords?.visibility = View.VISIBLE
                binding?.recyclerViewSearch?.visibility = View.VISIBLE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.GONE
                binding?.totalSearchRecords?.text = if (result.size > 1) {
                    String.format(
                        Locale.getDefault(), "%s %d %s", "About", result.size, "records..."
                    )
                } else {
                    String.format(Locale.getDefault(), "%s %d %s", "Only", result.size, "record")
                }
                //If we want to display normal listings like old version.
                /*binding?.recyclerViewSearch?.layoutManager =
                    LinearLayoutManager(activity)
                binding?.recyclerViewSearch?.adapter = SearchRecyclerViewAdapter(
                    result,
                    appPreferences
                )*/
            }
        }

        /**
         * Observe this for only revenue, expenses (as this livedata holds search results for reports to print, export)
         * Now: We are displaying expandable search results like monthly reports as these are easy to visualise.
         */
        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                DataModels.MonthlyReportData.Empty -> {
                    binding?.revenueDetails?.visibility = View.GONE
                    binding?.recyclerViewSearch?.visibility = View.GONE
                }

                is DataModels.MonthlyReportData.Data -> {
                    if (result.allExpensesOfThisMonth.isNotEmpty()) {
                        binding?.revenueDetails?.visibility = View.VISIBLE
                        binding?.recyclerViewSearch?.visibility = View.VISIBLE
                        setRevenueDetails(result.revenuesAmount, result.expensesAmount)
                        configureRecyclerView(
                            binding?.recyclerViewSearch!!, MainAdapter(
                                result.allExpensesParentList,
                                appPreferences,
                                onBannerClick = { banner ->
                                    /* val bundle = Bundle().apply {
                                         putString("banner_name", banner.app_name)
                                         putString("app_package", banner.package_name)
                                     }
                                     logAnalyticEvent("banner_clicked", bundle)*/
                                    if (banner.redirectUrl != null) {
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, banner.redirectUrl!!.toUri())
                                        startActivity(intent)
                                    }
                                })/*MonthlyReportRecyclerViewAdapter(
                                result.expenses,
                                result.revenues,
                                result.allExpensesOfThisMonth,
                                appPreferences
                            )*/
                        )
                    }
                }
            }
        }

        /**
         * Loading
         */
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding?.searchExpensesProgressBar?.visibility =
                if (loading) View.VISIBLE else View.GONE
        }

        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding?.adViewContainer?.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

        /**
         * Handle search expenses
         */
        binding?.searchEditText?.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            binding?.ivClearTick?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            if (query.isEmpty()) viewModel.loadThisMonthExpenses()
        }


        /**
         * Clear search
         */
        binding?.ivClearTick?.setOnClickListener {
            binding?.searchEditText?.text?.clear()
            resetToDefaultChipThisMonth()
        }


        /**
         * Handle action done for search expenses
         */
        binding?.searchEditText?.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding?.searchEditText?.text.toString().trim()
                viewModel.searchExpenses(query)
                Keyboard.hideSoftKeyboard(requireContext(), binding?.searchEditText!!)
                binding?.chipReset?.visibility = View.GONE
                binding?.chipGroup?.clearCheck()
                binding?.searchResultsFor?.visibility = View.VISIBLE
                binding?.searchResultsFor?.text =
                    String.format(getString(R.string.search_results_for_), query)
                return@OnEditorActionListener true
            }
            false
        })

        binding?.searchResultsFor?.visibility = View.VISIBLE
        binding?.searchResultsFor?.text =
            String.format(getString(R.string.search_results_for_), SearchUtil.THIS_MONTH)
        //Add top searches
        SearchUtil.getTopSearches().forEach {
            addChipsForTopSearches(it)
        }
        /**
         *
         */
        binding?.chipReset?.setOnClickListener {
            chipResetAndReloadThisMonthExpenses()
        }

        /**
         * Export for this month
         */
        binding?.exportReport?.setOnClickListener {
            if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
                exportSelectionDialog()
            } else {
                showInterstitial()
            }
        }

        /*
         Observe HTML/PDF report generation
        */
        viewModel.observeGeneratePDFReport.observe(viewLifecycleOwner) { htmlReport ->
            htmlReport?.let {
                if (htmlReport.html.isEmpty() || htmlReport.isEmpty) {
                    toastManager.showShort(getString(R.string.please_add_expenses_for_this_month_to_generate_report))
                } else {
                    startActivity(
                        Intent(requireActivity(), PDFReportActivity::class.java).putExtra(
                            PDFReportActivity.INTENT_CODE_PDF_CONTENTS,
                            htmlReport.html
                        )
                    )
                }
            }
        }

        /*
          Observe export status
         */
        viewModel.observeExportStatus.observe(viewLifecycleOwner) { result ->
            //Get data list update it to UI, notify scroll down
            result?.let {
                if (it.message.isNotEmpty()) toastManager.showShort(it.message)
                if (!it.status) return@let

                it.file?.let { file ->
                    shareCsvFile(file)
                }
            }
        }

        /**
         * Search help / disclaimer
         */
        binding?.tvSearchDisclaimer?.setOnClickListener {
            DialogUtil.createDialog(
                requireContext(),
                message = getString(R.string.search_disclaimer),
                title = getString(R.string.about_search_box),
                isCancelable = false,
                positiveBtn = getString(R.string.noted),
                positiveClickListener = {},
                negativeBtn = "",
                negativeClickListener = {}).show()
        }
    }


    /**
     * Chip reset and reload expenses of this monthh
     */
    fun chipResetAndReloadThisMonthExpenses() {
        resetToDefaultChipThisMonth()
        viewModel.loadThisMonthExpenses()
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

    /**
     * Set revenues , expenses details
     */
    private fun setRevenueDetails(revenuesAmount: Double, expensesAmount: Double) {
        try {
            val rev = CurrencyHelper.getFormattedCurrencyString(appPreferences, revenuesAmount)
            val exp = CurrencyHelper.getFormattedCurrencyString(appPreferences, expensesAmount)
            val sentence = "Revenues: $rev, Expenses: $exp"
            // Create a SpannableString
            val spannableString = SpannableString(sentence)
            // Define the start and end indices for each word
            val colorfulWords = mapOf(
                "Revenues: " to "#4CAF50".toColorInt(), // Green
                "Expenses: " to "#F44336".toColorInt(), // Red
            )
            // Apply ForegroundColorSpan to each word
            for ((word, color) in colorfulWords) {
                val startIndex = sentence.indexOf(word)
                val endIndex = startIndex + word.length
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // Set the SpannableString to the TextView
            binding?.revenueDetails?.text = spannableString
        } catch (e: Exception) {
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

    // Called when the fragment is no longer in use. This is called after onStop() and before onDetach().
    override fun onDestroy() {
        adView?.destroy()
        mInterstitialAd = null
        super.onDestroy()
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
            (EzPermission.isGranted(
                requireContext(),
                storagePermissions[0]
            ) && EzPermission.isGranted(requireContext(), storagePermissions[1]))
        }
    }

    /**
     *
     */
    private fun askStoragePermission() {
        EzPermission.with(requireContext())
            .permissions(storagePermissions[0], storagePermissions[1])
            .request { granted, denied, permanentlyDenied ->
                if (granted.contains(storagePermissions[0]) && granted.contains(storagePermissions[1])) { // Storage permissions already Granted
                    exportSelectionDialog()
                } else if (denied.contains(storagePermissions[0]) || denied.contains(
                        storagePermissions[1]
                    )
                ) { // Denied
                    showStorageDeniedDialog()
                } else if (permanentlyDenied.contains(storagePermissions[0]) || permanentlyDenied.contains(
                        storagePermissions[1]
                    )
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
            "Download or print pdf", "Share spreadsheet"
        )
        val monthFormat = DateTimeFormatter.ofPattern(
            "MMM yyyy", Locale.getDefault()
        )
        MaterialAlertDialogBuilder(requireContext()).setTitle(
            String.format(
                "Budget report of %s",
                monthFormat.format(date)
            )
        ).setItems(weeks) { dialog, which ->
            if (which == 0) {
                viewModel.generateHtml(appPreferences.getUserCurrency(), date)
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_PRINT_PDF_REPORT
                )
            } else {
                exportExcel()
                //Log event
                analyticsManager.logEvent(Events.KEY_PRINT_EXCEL_REPORT)
            }
            dialog.dismiss()
        }.setPositiveButton("Not now") { dialog, p1 ->
            dialog.dismiss()
        }.setCancelable(false).show()
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
                    "MMMyyyy", Locale.getDefault()
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
            requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider", file
        )
        intentShareCSV(requireActivity(), fileUri)
    }

    /**
     * Call this for clear / reset chip click
     */
    private fun resetToDefaultChipThisMonth() {
        binding?.chipReset?.visibility = View.GONE
        binding?.chipGroup?.clearCheck()
        binding?.searchResultsFor?.visibility = View.VISIBLE
        binding?.searchResultsFor?.text =
            String.format(getString(R.string.search_results_for_), SearchUtil.THIS_MONTH)
        binding?.chipGroup?.findViewWithTag<Chip>(SearchUtil.THIS_MONTH)?.let {
            it.isChecked = true
        }
        binding?.searchEditText?.text?.clear()
        //Log event
        analyticsManager.logEvent(
            Events.KEY_SEARCH_FILTER, mapOf(
                Events.KEY_VALUE to Events.KEY_SEARCH_RESET
            )
        )
    }


    /**
     * Add hardcoded chips for Top searches
     */
    private fun addChipsForTopSearches(
        chipText: String,
        showClose: Boolean = false,
        isChecked: Boolean = false,
    ) {
        val chip = Chip(requireContext())
        chip.text = chipText
        chip.tag = chipText
        chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_download_app)
        chip.isChipIconVisible = false
        chip.isCloseIconVisible = (showClose && chipText != ExpenseCategoryType.MISCELLANEOUS.name)
        // necessary to get single selection working
        chip.isClickable = true
        binding?.chipGroup?.isSingleSelection = !showClose
        chip.isCheckable = !showClose
        chip.isChecked = if (chipText == SearchUtil.THIS_MONTH) true else isChecked
        binding?.chipGroup?.addView(chip as View)

        chip.setOnClickListener {
            val clickChipText = (it as Chip).text ?: ""
            binding?.searchResultsFor?.visibility = View.VISIBLE
            binding?.chipReset?.visibility = View.VISIBLE
            binding?.searchResultsFor?.text =
                String.format(getString(R.string.search_results_for_), clickChipText)
            when (clickChipText) {
                SearchUtil.TODAY -> {
                    viewModel.loadTodayExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.TODAY
                        )
                    )
                }

                SearchUtil.YESTERDAY -> {
                    viewModel.loadYesterdayExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.YESTERDAY
                        )
                    )
                }

                SearchUtil.TOMORROW -> {
                    viewModel.loadTomorrowExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.TOMORROW
                        )
                    )
                }

                SearchUtil.THIS_WEEK -> {
                    viewModel.loadThisWeekExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.THIS_WEEK
                        )
                    )
                }

                SearchUtil.LAST_WEEK -> {
                    viewModel.loadLastWeekExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.LAST_WEEK
                        )
                    )
                }

                SearchUtil.THIS_MONTH -> {
                    viewModel.loadThisMonthExpenses()
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.THIS_MONTH
                        )
                    )
                }

                SearchUtil.PICK_A_DATE -> {
                    requireActivity().pickSingleDate(onDateSet = { date ->
                        viewModel.loadExpensesForADate(date)
                        binding?.searchResultsFor?.visibility = View.VISIBLE
                        binding?.searchResultsFor?.text = String.format(
                            getString(R.string.search_results_for_), dayFormatter.format(date)
                        )
                    })
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.PICK_A_DATE
                        )
                    )
                }

                SearchUtil.PICK_A_DATE_RANGE -> {
                    requireActivity().pickDateRange(onDateSet = { dates ->
                        viewModel.loadExpensesForGivenDates(dates.first, dates.second)
                        binding?.searchResultsFor?.visibility = View.VISIBLE
                        binding?.searchResultsFor?.text = String.format(
                            getString(R.string.search_results_for_range),
                            dayFormatter.format(dates.first),
                            dayFormatter.format(dates.second)
                        )
                    })
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_SEARCH_FILTER, mapOf(
                            Events.KEY_VALUE to SearchUtil.PICK_A_DATE_RANGE
                        )
                    )
                }
            }
        }
    }

    /**
     * SearchFragment
     */
    companion object {
        fun newInstance(): SearchFragment = SearchFragment()
    }

    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            if (InternetUtils.isInternetAvailable(requireActivity()).not()) return
            binding?.adViewContainer?.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                requireContext(), requireActivity().windowManager.defaultDisplay
            )!!
            adView = AdView(requireContext())
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            binding?.adViewContainer?.addView(adView)
            val actualAdRequest = AdRequest.Builder().build()
            adView?.setAdSize(adSize)
            adView?.loadAd(actualAdRequest)
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
                override fun onAdClosed() {
                    loadAndDisplayBannerAds()
                }
            }
        } catch (e: Exception) {
            Logger.error(getString(R.string.error_while_displaying_banner_ad), e)
        }
    }
}
