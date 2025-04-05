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
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.InternetUtils
import com.simplebudget.helper.Logger
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.view.category.choose.ChooseCategoryActivity
import com.simplebudget.view.settings.webview.WebViewActivity
import org.koin.android.ext.android.inject

class FAQActivity : BaseActivity<ActivityFaqBinding>() {


    private lateinit var faqAdapter: FAQAdapter
    private var faqsList: ArrayList<Question> = ArrayList()
    private val analyticsManager: AnalyticsManager by inject()
    private val toastManager: ToastManager by inject()

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

        // Screen name event
        analyticsManager.logEvent(Events.KEY_FAQ_SCREEN)

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

        // Open how to
        binding.checkHowTo.setOnClickListener {
            if (InternetUtils.isInternetAvailable(this)) {
                analyticsManager.logEvent(Events.KEY_HOW_TO)
                WebViewActivity.start(
                    this,
                    getString(R.string.simple_budget_how_to_url),
                    getString(R.string.setting_how_to_title),
                    false
                )
            } else {
                toastManager.showShort(getString(R.string.no_internet_connection))
            }
        }
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
        try {
            binding.voiceSearchQuery.setOnClickListener {
                analyticsManager.logEvent(Events.KEY_FAQ_VOICE_SEARCHED)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                }
                voiceSearchIntentLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Logger.error(
                ChooseCategoryActivity::class.java.simpleName,
                "Error in searching FAQ using voice ${e.localizedMessage}",
                e
            )
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
