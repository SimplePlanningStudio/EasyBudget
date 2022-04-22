package com.simplebudget.view.welcome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplebudget.R
import com.simplebudget.databinding.FragmentOnboarding2Binding
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.helper.getUserCurrency
import com.simplebudget.view.selectcurrency.SelectCurrencyFragment
import org.koin.android.ext.android.inject
import java.util.*

/**
 * Onboarding step 2 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding2Fragment : OnboardingFragment<FragmentOnboarding2Binding>() {
    private lateinit var selectedCurrency: Currency
    private lateinit var receiver: BroadcastReceiver

    private val appPreferences: AppPreferences by inject()

    override val statusBarColor: Int
        get() = R.color.primary

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedCurrency = appPreferences.getUserCurrency()
        setNextButtonText()

        val selectCurrencyFragment = SelectCurrencyFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.expense_select_container, selectCurrencyFragment).commit()

        val filter = IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                selectedCurrency =
                    Currency.getInstance(intent.getStringExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA))
                setNextButtonText()
            }
        }

        LocalBroadcastManager.getInstance(view.context).registerReceiver(receiver, filter)

        binding?.onboardingScreen2NextButton?.setOnClickListener {
            next()
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)

        super.onDestroyView()
    }

    /**
     * Set the next button text according to the selected currency
     */
    private fun setNextButtonText() {
        //onboarding_screen2_next_button?.text = resources.getString(R.string.onboarding_screen_2_cta, selectedCurrency.symbol)
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding2Binding = FragmentOnboarding2Binding.inflate(inflater, container, false)
}
