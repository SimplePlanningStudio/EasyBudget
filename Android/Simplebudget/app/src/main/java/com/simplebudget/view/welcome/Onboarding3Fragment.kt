package com.simplebudget.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.model.Expense
import com.simplebudget.prefs.AppPreferences
import kotlinx.android.synthetic.main.fragment_onboarding3.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.util.*

/**
 * Onboarding step 3 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding3Fragment : OnboardingFragment(), CoroutineScope by MainScope() {
    private val appPreferences: AppPreferences by inject()

    override val statusBarColor: Int
        get() = R.color.primary

    private val amountValue: Double
        get() {
            val valueString = onboarding_screen3_initial_amount_et.text.toString()

            return try {
                if ("" == valueString || "-" == valueString) 0.0 else java.lang.Double.valueOf(valueString)
            } catch (e: Exception) {
                val context = context ?: return 0.0

                AlertDialog.Builder(context)
                        .setTitle(R.string.adjust_balance_error_title)
                        .setMessage(R.string.adjust_balance_error_message)
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()

                Logger.warning("An error occurred during initial amount parsing: $valueString", e)
                return 0.0
            }

        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launch {
            val amount = withContext(Dispatchers.Default) {
                -db.getBalanceForDay(Date())
            }

            onboarding_screen3_initial_amount_et.setText(if (amount == 0.0) "" else amount.toString())
        }

        setCurrency()

        onboarding_screen3_initial_amount_et.preventUnsupportedInputForDecimals()
        onboarding_screen3_next_button.setOnClickListener {
            launch {
                withContext(Dispatchers.Default) {
                    val currentBalance = -db.getBalanceForDay(Date())
                    val newBalance = amountValue

                    if (newBalance != currentBalance) {
                        val diff = newBalance - currentBalance

                        val expense = Expense(resources.getString(R.string.adjust_balance_expense_title), -diff, Date())
                        db.persistExpense(expense)
                    }
                }

                Keyboard.hideSoftKeyboard(requireContext(), onboarding_screen3_initial_amount_et)

                next(onboarding_screen3_next_button)
            }
        }
    }

    override fun onDestroy() {
        cancel()

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        setCurrency()
    }

// -------------------------------------->

    private fun setCurrency() {
        onboarding_screen3_initial_amount_money_tv?.text = String.format("%s - %s", appPreferences.getUserCurrency().symbol, appPreferences.getUserCurrency().displayName)
    }
}