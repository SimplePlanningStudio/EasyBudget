package com.simplebudget.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.simplebudget.R
import com.simplebudget.databinding.FragmentOnboarding1Binding

/**
 * Onboarding step 1 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding1Fragment : OnboardingFragment<FragmentOnboarding1Binding>() {

    override val statusBarColor: Int
        get() = R.color.primary_dark

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.onboardingScreen1NextButton?.setOnClickListener {
            next(it)
        }
    }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding1Binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
}
