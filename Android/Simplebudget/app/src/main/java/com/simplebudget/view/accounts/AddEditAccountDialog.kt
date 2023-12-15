package com.simplebudget.view.accounts

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.simplebudget.R
import com.simplebudget.helper.DialogUtil
import com.simplebudget.helper.toast
import com.simplebudget.model.account.Account
import com.simplebudget.view.premium.PremiumActivity


/**
 *  updateAccount: (accountDetails: Triple<String, Boolean, Account?>) -> Unit
 *  Here:
 *
 *  String: 1st param is for new account name input by user
 *  Boolean: Return if it's editing and adding new new account
 *  Account: If editing it hold the the account to be edited otherwise it's null
 */
object AddEditAccountDialog {
    fun open(
        context: Activity,
        account: Account? = null,
        remainingAccounts: Int,
        addUpdateAccount: (accountDetails: Triple<String, Boolean, Account?>) -> Unit,
        isPremiumUser: Boolean
    ) {
        // Only premium users can add accounts
        if (isPremiumUser.not()) {
            DialogUtil.createDialog(context,
                title = context.getString(R.string.become_premium),
                message = context.getString(R.string.to_add_more_accounts_you_need_to_upgrade_to_premium),
                positiveBtn = context.getString(R.string.sure),
                negativeBtn = context.getString(R.string.cancel),
                isCancelable = true,
                positiveClickListener = {
                    val intent = Intent(context, PremiumActivity::class.java)
                    startActivity(context, intent, null)
                },
                negativeClickListener = {}).show()
            return
        }

        if (account == null && remainingAccounts <= 0) {
            // Not editing existing account but adding new
            context.toast(context.getString(R.string.you_have_already_added_five_accounts))
            return
        }
        context.let { activity ->
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_add_edit_account, null)
            val etAccount = dialogView.findViewById<View>(R.id.etAccount) as EditText
            etAccount.hint = account?.name ?: "Savings Account etc."

            val title = if (account == null) "Add Account" else "Edit ${account.name} Account"

            val description = if (account == null) {
                // Adding new
                String.format(
                    "%s %s",
                    "You can add $remainingAccounts",
                    if (remainingAccounts > 1) "more Accounts!" else "more Account!"
                )
            } else {
                // Editing
                String.format("%s", "You can only edit account name!")
            }

            val builder = AlertDialog.Builder(activity).setTitle(title).setMessage(description)
                .setView(dialogView)
                .setPositiveButton(if (account == null) R.string.save else R.string.update) { dialog, _ ->
                    val newAccountName = etAccount.text.toString()
                    if (newAccountName.trim { it <= ' ' }.isEmpty()) {
                        context.toast(context.getString(R.string.account_cant_be_empty))
                    } else {
                        addUpdateAccount.invoke(
                            Triple(
                                newAccountName.trim().uppercase(),
                                (account != null),
                                account
                            )
                        )
                    }
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

            val dialog = builder.show()
            // Directly show keyboard when the dialog pops
            etAccount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Check if the device doesn't have a physical keyboard
                if (hasFocus && activity.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        }
    }
}