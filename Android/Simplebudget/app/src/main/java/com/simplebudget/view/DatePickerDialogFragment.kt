/*
 *   Copyright 2025 Benoit LETONDOR
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
package com.simplebudget.view

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.simplebudget.helper.toStartOfDayDate
import java.time.LocalDate

class DatePickerDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_DATE = "arg_date"
        private const val ARG_MIN_DATE = "arg_min_date"

        fun newInstance(
            originalDate: LocalDate,
            minDateOverride: LocalDate? = null,
        ): DatePickerDialogFragment {
            val fragment = DatePickerDialogFragment()
            val args = Bundle()
            args.putString(ARG_DATE, originalDate.toString())
            minDateOverride?.let { args.putString(ARG_MIN_DATE, it.toString()) }
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var listener: DatePickerDialog.OnDateSetListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = LocalDate.parse(requireArguments().getString(ARG_DATE))
        val minDate: LocalDate? =
            if (requireArguments().containsKey(ARG_MIN_DATE))
                requireArguments().getString(ARG_MIN_DATE)?.let { LocalDate.parse(it) }
            else
                null

        val dialog = DatePickerDialog(
            requireContext(),
            listener,
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        if (minDate != null) {
            dialog.datePicker.minDate = minDate.toStartOfDayDate().time
        }
        return dialog
    }
}
