package com.simplebudget.view.settings.faq

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import com.simplebudget.R
import com.simplebudget.databinding.ActivityFaqBinding
import com.simplebudget.helper.BaseActivity

class FAQActivity : BaseActivity<ActivityFaqBinding>() {


    private lateinit var faqAdapter: FAQAdapter
    private var faqsList: ArrayList<Question> = ArrayList()

    /**
     *
     */
    override fun createBinding(): ActivityFaqBinding =
        ActivityFaqBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        faqsList.addAll(FaqRepo().faqList)
        attachAdapter(faqsList)

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().uppercase()
            filterWithQuery(query)
            toggleImageView(query)
        }

        handleVoiceSearch()
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Start activity for result
     */
    private var voiceSearchIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    .let { results ->
                        results?.get(0)
                    }
            binding.searchEditText.setText(spokenText ?: "")
        }

    /**
     * Handle Voice Search Activity
     */
    private fun handleVoiceSearch() {
        binding.voiceSearchQuery.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
            voiceSearchIntentLauncher.launch(intent)
        }
    }

    /**
     *
     */
    private fun filterWithQuery(query: String) {
        if (query.trim().isNotEmpty()) {
            val filteredList: List<Question> = onFilterChanged(query)
            attachAdapter(filteredList, true)
            toggleRecyclerView(filteredList)
        } else if (query.trim().isEmpty()) {
            attachAdapter(faqsList)
        }
    }

    /**
     *
     */
    private fun onFilterChanged(filterQuery: String): List<Question> {
        val filteredList = ArrayList<Question>()
        for (question in faqsList) {
            if (question.question.uppercase().contains(filterQuery.uppercase()) ||
                question.answer.uppercase().contains(filterQuery.uppercase())
            ) {
                filteredList.add(question)
            }
        }
        return filteredList
    }


    /**
     *
     */
    private fun toggleRecyclerView(faqs: List<Question>) {
        if (faqs.isEmpty()) {
            binding.recyclerView.visibility = View.INVISIBLE
            binding.tvNotFound.visibility = View.VISIBLE
            binding.tvNotFound.text = String.format(
                getString(R.string.no_category_found),
                binding.searchEditText.text.toString()
            )
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvNotFound.visibility = View.GONE
        }
    }

    /**
     *
     */
    private fun attachAdapter(list: List<Question>, allExpanded: Boolean = false) {
        faqAdapter = FAQAdapter(list, allExpanded)
        binding.recyclerView.adapter = faqAdapter
    }

    /**
     *
     */
    private fun toggleImageView(query: String) {
        if (query.isNotEmpty()) {
            binding.voiceSearchQuery.visibility = View.INVISIBLE
        } else if (query.isEmpty()) {
            binding.voiceSearchQuery.visibility = View.VISIBLE
        }
    }

}
