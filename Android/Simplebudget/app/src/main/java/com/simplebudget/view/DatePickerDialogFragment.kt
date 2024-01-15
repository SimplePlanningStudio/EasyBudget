/*
 *   Copyright 2024 Benoit LETONDOR
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
import com.simplebudget.helper.computeCalendarMinDateFromInitDate
import com.simplebudget.helper.toStartOfDayDate
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitDate
import org.koin.android.ext.android.inject
import java.time.LocalDate
import javax.inject.Inject


class DatePickerDialogFragment(
    private val originalDate: LocalDate,
    private val listener: DatePickerDialog.OnDateSetListener,
) : DialogFragment() {

    private val appPreferences: AppPreferences by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a new instance of DatePickerDialog and return it
        val dialog = DatePickerDialog(
            requireContext(),
            listener,
            originalDate.year,
            originalDate.monthValue - 1,
            originalDate.dayOfMonth
        )

        dialog.datePicker.minDate =
            (appPreferences.getInitDate() ?: LocalDate.now()).computeCalendarMinDateFromInitDate()
                .toStartOfDayDate().time
        return dialog
    }
}
