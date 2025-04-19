package com.simplebudget.helper

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.simplebudget.databinding.DialogSingleChoiceBinding

class CustomSingleChoiceDialog : DialogFragment() {
    private lateinit var binding: DialogSingleChoiceBinding
    private var selectedPosition = 0

    companion object {
        fun show(
            manager: FragmentManager,
            title: String,
            message: String? = null,
            options: List<String>,
            selectedIndex: Int = 0,
            onSave: (Int, String) -> Unit,
            onCancel: () -> Unit = {},
            onMessageUpdate: ((position: Int, selectedItem: String) -> String)? = null,
        ) {
            CustomSingleChoiceDialog().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("message", message)
                    putStringArrayList("options", ArrayList(options))
                    putInt("selectedIndex", selectedIndex)
                }
                this.onSave = onSave
                this.onCancel = onCancel
                this.onMessageUpdate = onMessageUpdate
            }.show(manager, "CustomSingleChoiceDialog")
        }
    }

    private var onSave: ((Int, String) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    private var onMessageUpdate: ((Int, String) -> String)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSingleChoiceBinding.inflate(layoutInflater)
        val args = requireArguments()
        val options = args.getStringArrayList("options") ?: arrayListOf()
        selectedPosition = args.getInt("selectedIndex", 0)

        setupViews(args, options)
        setupButtons()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(android.R.color.white)
            }
    }

    private fun setupViews(args: Bundle, options: List<String>) {
        binding.apply {
            dialogTitle.text = args.getString("title")

            args.getString("message")?.let {
                dialogMessage.text = it
                dialogMessage.visibility = View.VISIBLE
            }

            optionsList.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                options
            )

            optionsList.choiceMode = ListView.CHOICE_MODE_SINGLE
            optionsList.setItemChecked(selectedPosition, true)

            optionsList.setOnItemClickListener { _, _, position, _ ->
                selectedPosition = position
                val selectedItem = options[position]
                // Update message based on selection
                onMessageUpdate?.let { updater ->
                    updateMessage(updater(position, selectedItem))
                }
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            cancelButton.setOnClickListener {
                onCancel?.invoke()
                dismiss()
            }

            saveButton.setOnClickListener {
                val selectedItem =
                    (optionsList.adapter as ArrayAdapter<String>).getItem(selectedPosition)
                if (selectedItem != null) {
                    onSave?.invoke(selectedPosition, selectedItem)
                }
                dismiss()
            }
        }
    }

    // Add this to your CustomSingleChoiceDialog class
    fun updateMessage(newMessage: String) {
        if (binding.dialogMessage.isVisible) {
            binding.dialogMessage.apply {
                text = newMessage
                visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onSave = null
        onCancel = null
    }
}