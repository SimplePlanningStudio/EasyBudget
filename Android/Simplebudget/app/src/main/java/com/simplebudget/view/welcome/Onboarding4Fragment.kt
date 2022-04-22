package com.simplebudget.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.simplebudget.R
import com.simplebudget.databinding.FragmentOnboarding4Binding

/**
 * Onboarding step 4 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding4Fragment : OnboardingFragment<FragmentOnboarding4Binding>() {

    override val statusBarColor: Int
        get() = R.color.primary_dark

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.onboardingScreen4NextButton?.setOnClickListener {
            done()
        }
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding4Binding = FragmentOnboarding4Binding.inflate(inflater, container, false)
}
