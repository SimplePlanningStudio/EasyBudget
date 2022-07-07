/*
 *   Copyright 2022 Waheed Nazir
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
package com.simplebudget.view.category

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.google.android.material.chip.Chip
import com.simplebudget.R
import com.simplebudget.databinding.BottomsheetDialogCategoriesBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.Keyboard
import com.simplebudget.model.ExpenseCategories
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.view.CategoriesViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel


class CategoriesActivity : BaseActivity<BottomsheetDialogCategoriesBinding>() {

    private var categoryUseCase = ""
    private var selectedChipName = ""
    private var showCloseIconOnChip = false
    private val viewModelCategory: CategoriesViewModel by viewModel()
    private var categories: ArrayList<String> = ArrayList()
    private lateinit var adapterCategory: ArrayAdapter<String>

    companion object {
        const val MANAGE_CATEGORIES = "MANAGE_CATEGORIES"
        const val REQUEST_CODE_SELECT_CATEGORY = "SELECT_CATEGORY "
    }

    /**
     *
     */
    override fun createBinding(): BottomsheetDialogCategoriesBinding {
        return BottomsheetDialogCategoriesBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setOrientation()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        this.window
            .setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        categoryUseCase = intent?.getStringExtra(REQUEST_CODE_SELECT_CATEGORY) ?: ""
        showCloseIconOnChip =
            (intent?.getStringExtra(REQUEST_CODE_SELECT_CATEGORY) ?: "") == MANAGE_CATEGORIES
        binding.ivCloseCategory.setOnClickListener { doneWithSelection() }
        binding.doneButton.text =
            if (showCloseIconOnChip) getString(R.string.done_with_editing) else getString(R.string.done_with_selection)
        binding.title.text =
            if (showCloseIconOnChip) getString(R.string.setting_category_manage_category_title) else getString(
                R.string.select_category
            )
        binding.doneButton.setOnClickListener { doneWithSelection() }
        binding.categoriesSpinner.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                //val DRAWABLE_LEFT = 0 // val DRAWABLE_TOP = 1 //val DRAWABLE_BOTTOM = 3
                val drawableRight = 2
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= binding.categoriesSpinner.right - binding.categoriesSpinner.compoundDrawables[drawableRight].bounds.width()) {
                        val chipText = binding.categoriesSpinner.text.toString().trim()
                        if (chipText.isNotEmpty())
                            addChipFromTextAndScrollDown(chipText.uppercase().trim())
                        return true
                    }
                }
                return false
            }
        })
        handleNewCategory()
        loadCategories()
        handleActionDone()
    }

    /**
     * Load categories from DB
     */
    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE
        //Load categories
        viewModelCategory.categoriesLiveData.observe(this) { dbCat ->
            categories.clear()
            if (dbCat.isEmpty()) {
                ExpenseCategories.getCategoriesList().forEach { item ->
                    if (!categories.contains(item.uppercase()))
                        categories.add(item.uppercase())
                }
            } else {
                categories.addAll(dbCat)
            }
            categories.forEach { chipText ->
                addChipForFirstTime(chipText.uppercase(), showCloseIconOnChip)
            }
            //Auto complete spinner view
            setCategoriesView()
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     *
     */
    private fun handleNewCategory() {
        binding.categoriesSpinner.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(chipText: Editable?) {

                if (chipText.toString().trim().isNotEmpty()) {
                    if (chipText.toString().length >= 3) {
                        binding.doneButton.visibility = View.VISIBLE
                    }
                }

            }

            override fun beforeTextChanged(
                chipText: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                chipText: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (chipText.toString().trim().isNotEmpty()) {
                    if (chipText?.length ?: 0 >= 3 && chipText?.last().toString() == ",") {
                        addChipToGroup(chipText.toString().uppercase(), showCloseIconOnChip)
                    }
                }

            }
        })
    }

    /**
     * Only to add chips for first time when loading from DB
     */
    private fun addChipForFirstTime(
        chipText: String,
        showClose: Boolean,
        isChecked: Boolean = false
    ) {
        val chip = Chip(this)
        chip.text = chipText
        chip.tag = chipText
        chip.chipIcon = ContextCompat.getDrawable(this, R.drawable.ic_download_app)
        chip.isChipIconVisible = false
        chip.isCloseIconVisible = (showClose && chipText != ExpenseCategoryType.MISCELLANEOUS.name)
        // necessary to get single selection working
        chip.isClickable = true
        binding.chipGroup.isSingleSelection = !showClose
        chip.isCheckable = !showClose
        chip.isChecked = isChecked
        binding.chipGroup.addView(chip as View)

        //Internal utils to remove chip
        fun removeChipView(chip: View, chipTextString: String) {
            binding.chipGroup.removeView(chip)
            viewModelCategory.deleteCategory(chipTextString)
            Toast.makeText(
                this,
                "${getString(R.string.removed)} $chipTextString",
                Toast.LENGTH_SHORT
            ).show()
        }

        chip.setOnCloseIconClickListener {
            val chipTextString = (it as Chip).text.toString()
            try {
                val builder = android.app.AlertDialog.Builder(this)
                builder.setCancelable(false)
                builder.setTitle("Remove '${chipTextString}'?")
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }.setPositiveButton("Yes") { dialog, _ ->
                        removeChipView(it, chipTextString)
                        dialog.cancel()
                    }
                val alertDialog = builder.create()
                alertDialog.show()
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(resources.getColor(R.color.budget_red))
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(resources.getColor(R.color.budget_green))
            } catch (e: Exception) {
                removeChipView(it, chipTextString)
            }
        }
        chip.setOnClickListener {
            binding.doneButton.visibility = if ((it as Chip).isChecked) View.VISIBLE else View.GONE
            selectedChipName = if (it.isChecked) it.text?.toString() ?: "" else ""
        }
        if (chip.isChecked) selectedChipName = chipText
    }

    /**
     *
     */
    private fun addChipToGroup(
        chipText: String,
        showClose: Boolean,
        isChecked: Boolean = false
    ) {
        if (categories.contains(chipText)) {
            //Now just highlight existing chip
            for (child in binding.chipGroup.children) {
                if ((child as Chip).text == chipText) {
                    child.isChecked = true
                    selectedChipName = chipText
                    //Scroll down for existing chip from input page
                    binding.chipsScrollview.post(Runnable {
                        binding.chipsScrollview.scrollTo(
                            child.scrollX,
                            child.scrollY
                        )
                    })
                    binding.doneButton.visibility = View.VISIBLE
                    Keyboard.hideSoftKeyboard(this, binding.categoriesSpinner)
                    break
                }
            }
        } else {
            categories.add(chipText)
            addChipForFirstTime(chipText, showClose, isChecked)
            //It's not exist in db it's new so add into DB as well
            addNewCategoryToDB(chipText)
        }
    }

    private fun addNewCategoryToDB(chipText: String) {
        viewModelCategory.saveCategory(chipText)
    }

    /**
     * Handle action done for entering category
     */
    private fun handleActionDone() {
        binding.categoriesSpinner.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val chipText = binding.categoriesSpinner.text.toString().trim()
                if (chipText.isNotEmpty())
                    addChipFromTextAndScrollDown(chipText.uppercase().trim())
                return@OnEditorActionListener true
            }
            false
        })
    }


    /**
     *
     */
    private fun addChipFromTextAndScrollDown(chipText: String) {
        if (chipText.length >= 3) {
            addChipToGroup(
                chipText,
                showCloseIconOnChip,
                true
            )
            //Scroll down for newly added chip from input page
            binding.chipsScrollview.post(Runnable {
                binding.chipsScrollview.fullScroll(
                    NestedScrollView.FOCUS_DOWN
                )
            })
            binding.doneButton.visibility = View.VISIBLE
            binding.categoriesSpinner.setText("")
            Keyboard.hideSoftKeyboard(this, binding.categoriesSpinner)
        }
    }

    /**
     *
     */
    private fun setCategoriesView() {
        adapterCategory = ArrayAdapter(
            this,
            R.layout.spinner_item_categories,
            categories
        )
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categoriesSpinner.setAdapter(adapterCategory)
        binding.categoriesSpinner.threshold = 1
        //Set category value if editing it would be other than MISCELLANEOUS or else MISCELLANEOUS
        binding.categoriesSpinner.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                addChipFromTextAndScrollDown(selectedItem.uppercase().trim())
            }
    }


    /**
     *
     */
    private fun setOrientation() {
        requestedOrientation = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     *
     */
    private fun doneWithSelection() {
        selectedChipName = if (selectedChipName.trim()
                .isEmpty()
        ) ExpenseCategoryType.MISCELLANEOUS.name else selectedChipName
        when (categoryUseCase) {
            REQUEST_CODE_SELECT_CATEGORY -> {
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(REQUEST_CODE_SELECT_CATEGORY, selectedChipName)
                )
                finish()
            }
            MANAGE_CATEGORIES -> {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            else -> {
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra(REQUEST_CODE_SELECT_CATEGORY, selectedChipName)
                )
                finish()
            }
        }
    }

    /**
     *
     */
    override fun onBackPressed() {
        doneWithSelection()
    }
}