package com.simplebudget.view.budgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.simplebudget.R
import com.simplebudget.base.BaseDialogFragment
import com.simplebudget.databinding.BottomSheetManageBudgetBinding
import com.simplebudget.helper.InternetUtils
import com.simplebudget.helper.Logger
import com.simplebudget.helper.ads.AdSdkManager
import com.simplebudget.helper.ads.NativeTemplateStyle
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.beVisible
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.budget.Budget
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject
import kotlin.getValue

class ManageBudgetBottomSheet(
    private val budget: Budget,
    private val onActionSelected: (Action) -> Unit,
) : BaseDialogFragment<BottomSheetManageBudgetBinding>() {

    companion object{
        const val TAG = "ManageBudgetBottomSheet"
    }

    private val appPreferences: AppPreferences by inject()
    private var nativeAd: NativeAd? = null

    enum class Action { TRANSACTIONS, EDIT, DELETE }

    override fun onCreateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): BottomSheetManageBudgetBinding =
        BottomSheetManageBudgetBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.budgetTitle?.text = getString(R.string.budget_name, budget.goal)
        binding?.optionTransactions?.setOnClickListener {
            onActionSelected(Action.TRANSACTIONS)
            dismiss()
        }

        binding?.optionEdit?.setOnClickListener {
            onActionSelected(Action.EDIT)
            dismiss()
        }

        binding?.optionDelete?.setOnClickListener {
            onActionSelected(Action.DELETE)
            dismiss()
        }
        binding?.ivClose?.setOnClickListener { dismiss() }

        //Load Ad
        if (appPreferences.isUserPremium().not() && InternetUtils.isInternetAvailable(requireContext())) {
            binding?.nativeAdTemplate?.beVisible()
            AdSdkManager.initialize(requireContext()) {
                //Load native ads
                loadNativeAd()
            }
        } else {
            binding?.nativeAdTemplate?.beGone()
        }
    }
    /**
     * Load native ads
     */
    private fun loadNativeAd() {
        try {
            val builder = AdLoader.Builder(requireActivity(), getString(R.string.native_ad_unit_id))
            builder.forNativeAd { nativeAd ->
                populateNativeAdView(nativeAd)
            }
            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Logger.warning("NativeAd", "Failed to load: ${loadAdError.message}")
                    nativeAd = null
                }
            }).build()
            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Logger.error("NativeAd", "Failed to load: ${e.localizedMessage}", e)
        }
    }

    /**
     *
     */
    private fun populateNativeAdView(nativeAd: NativeAd) {
        try {
            val styles = NativeTemplateStyle.Builder().withMainBackgroundColor(requireActivity().getColor(R.color.white).toDrawable()
            ).build()
            binding?.nativeAdTemplate?.setStyles(styles)
            binding?.nativeAdTemplate?.setNativeAd(nativeAd)
            this.nativeAd = nativeAd
        } catch (e: Exception) {
            Logger.error("NativeTemplateStyle", "Failed to populate: ${e.localizedMessage}", e)
        }
    }

    /**
     * Destroy native ads
     */
    override fun onDestroyView() {
        this.nativeAd?.destroy()
        this.nativeAd = null
        super.onDestroyView()
    }
}

