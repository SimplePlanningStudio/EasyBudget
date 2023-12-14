package com.simplebudget.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import com.simplebudget.databinding.FragmentOnboarding3Binding
import com.simplebudget.helper.*
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.time.LocalDate

/**
 * Onboarding step 3 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding3Fragment : OnboardingFragment<FragmentOnboarding3Binding>(),
    CoroutineScope by MainScope() {
    private val appPreferences: AppPreferences by inject()

    override val statusBarColor: Int
        get() = R.color.primary

    private val amountValue: Double
        get() {
            val valueString = binding?.onboardingScreen3InitialAmountEt?.text.toString()

            return try {
                if ("" == valueString || "-" == valueString) 0.0 else java.lang.Double.valueOf(
                    valueString
                )
            } catch (e: Exception) {
                val context = context ?: return 0.0

                AlertDialog.Builder(context)
                    .setTitle(R.string.oops)
                    .setMessage(R.string.adjust_balance_error_message)
                    .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()

                Logger.warning("An error occurred during initial amount parsing: $valueString", e)
                return 0.0
            }

        }

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launch {
            val amount = withContext(Dispatchers.Default) {
                -db.getBalanceForDay(LocalDate.now(), appPreferences.activeAccount())
            }

            binding?.onboardingScreen3InitialAmountEt?.setText(if (amount == 0.0) "" else amount.toString())
        }

        setCurrency()

        binding?.onboardingScreen3InitialAmountEt?.preventUnsupportedInputForDecimals()
        binding?.onboardingScreen3NextButton?.setOnClickListener {
            launch {
                withContext(Dispatchers.Default) {
                    val currentBalance =
                        -db.getBalanceForDay(LocalDate.now(), appPreferences.activeAccount())
                    val newBalance = amountValue

                    if (newBalance != currentBalance) {
                        val diff = newBalance - currentBalance

                        val expense = Expense(
                            resources.getString(R.string.adjust_balance_expense_title),
                            -diff,
                            LocalDate.now(),
                            ExpenseCategoryType.BALANCE.name,
                            appPreferences.activeAccount()
                        )
                        db.persistExpense(expense)
                    }
                }

                Keyboard.hideSoftKeyboard(
                    requireContext(),
                    binding?.onboardingScreen3InitialAmountEt!!
                )

                next(binding?.onboardingScreen3NextButton!!)
            }
        }
    }

    /**
     *
     */
    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    /**
     *
     */
    override fun onResume() {
        super.onResume()
        setCurrency()
    }

    /**
     *
     */
    private fun setCurrency() {
        binding?.onboardingScreen3InitialAmountMoneyTv?.text = String.format(
            "%s - %s",
            appPreferences.getUserCurrency().symbol,
            appPreferences.getUserCurrency().displayName
        )
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding3Binding = FragmentOnboarding3Binding.inflate(inflater, container, false)
}