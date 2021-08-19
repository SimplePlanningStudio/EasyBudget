package com.simplebudget.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.simplebudget.R
import kotlinx.android.synthetic.main.fragment_onboarding1.*

/**
 * Onboarding step 1 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding1Fragment : OnboardingFragment() {

    override val statusBarColor: Int
        get() = R.color.primary_dark

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboarding_screen1_next_button.setOnClickListener {
            next(onboarding_screen1_next_button)
        }
    }
}
