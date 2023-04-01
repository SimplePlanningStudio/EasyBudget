/*
 *   Copyright 2023 Benoit LETONDOR
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
package com.simplebudget.view.welcome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import com.simplebudget.databinding.ActivityWelcomeBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.setStatusBarColor
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.main.MainActivity
import java.lang.IllegalStateException
import kotlin.math.max
import org.koin.android.ext.android.inject

/**
 * Welcome screen activity
 *
 * @author Benoit LETONDOR
 */
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    /**
     * Broadcast receiver for intent sent by fragments
     */
    private lateinit var receiver: BroadcastReceiver

    private val appPreferences: AppPreferences by inject()

// ------------------------------------>

    private var step: Int
        get() = appPreferences.getOnboardingStep()
        set(step) = appPreferences.setOnboardingStep(step)

// ------------------------------------------>

    override fun createBinding(): ActivityWelcomeBinding =
        ActivityWelcomeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {

        val isAndroid13OrMore = Build.VERSION.SDK_INT >= 33

        // Reinit step to 0 if already completed
        if (step == STEP_COMPLETED) {
            step = 0
        }
        super.onCreate(savedInstanceState)
        binding.welcomeViewPager.adapter = object : FragmentStatePagerAdapter(
            supportFragmentManager,
            BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            override fun getItem(position: Int): Fragment {
                if (isAndroid13OrMore) {
                    when (position) {
                        0 -> return Onboarding1Fragment()
                        1 -> return Onboarding2Fragment()
                        2 -> return Onboarding3Fragment()
                        3 -> return OnboardingPushPermissionFragment()
                        4 -> return Onboarding4Fragment()
                    }
                } else {
                    when (position) {
                        0 -> return Onboarding1Fragment()
                        1 -> return Onboarding2Fragment()
                        2 -> return Onboarding3Fragment()
                        3 -> return Onboarding4Fragment()
                    }
                }

                throw IllegalStateException("unknown position $position")
            }

            override fun getCount(): Int = if (Build.VERSION.SDK_INT >= 33) 5 else 4
        }
        binding.welcomeViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                ((binding.welcomeViewPager.adapter as? FragmentStatePagerAdapter)?.getItem(position) as? OnboardingFragment<*>)?.let { fragment ->
                    setStatusBarColor(fragment.statusBarColor)
                }

                step = position
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.welcomeViewPager.offscreenPageLimit = binding.welcomeViewPager.adapter?.count
            ?: 0 // preload all fragments for transitions smoothness

        // Circle indicator
        binding.welcomeViewPagerIndicator.setViewPager(binding.welcomeViewPager)

        val filter = IntentFilter()
        filter.addAction(PAGER_NEXT_INTENT)
        filter.addAction(PAGER_PREVIOUS_INTENT)
        filter.addAction(PAGER_DONE_INTENT)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pager = binding.welcomeViewPager ?: return
                val pagerAdapter = pager.adapter ?: return

                if (PAGER_NEXT_INTENT == intent.action && pager.currentItem < pagerAdapter.count - 1) {
                    if (intent.getBooleanExtra(
                            ANIMATE_TRANSITION_KEY,
                            false
                        ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    ) {
                        // get the center for the clipping circle
                        val cx = intent.getIntExtra(CENTER_X_KEY, pager.x.toInt() + pager.width / 2)
                        val cy =
                            intent.getIntExtra(CENTER_Y_KEY, pager.y.toInt() + pager.height / 2)

                        // get the final radius for the clipping circle
                        val finalRadius = max(pager.width, pager.height)

                        // create the animator for this view (the start radius is zero)
                        val anim = ViewAnimationUtils.createCircularReveal(
                            pager,
                            cx,
                            cy,
                            0f,
                            finalRadius.toFloat()
                        )

                        // make the view visible and start the animation
                        pager.setCurrentItem(pager.currentItem + 1, false)
                        anim.start()
                    } else {
                        pager.setCurrentItem(pager.currentItem + 1, true)
                    }
                } else if (PAGER_PREVIOUS_INTENT == intent.action && pager.currentItem > 0) {
                    pager.setCurrentItem(pager.currentItem - 1, true)
                } else if (PAGER_DONE_INTENT == intent.action) {
                    step = STEP_COMPLETED
                    startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        val initialStep = step

        // Init pager at the current step
        binding.welcomeViewPager.setCurrentItem(initialStep, false)

        // Set status bar color
        (((binding.welcomeViewPager.adapter) as? FragmentStatePagerAdapter)?.getItem(initialStep) as? OnboardingFragment<*>)?.let { fragment ->
            setStatusBarColor(fragment.statusBarColor)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)

        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.welcomeViewPager.currentItem > 0) {
            binding.welcomeViewPager.setCurrentItem(binding.welcomeViewPager.currentItem - 1, true)
            return
        }
        startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
        finish()
    }

    companion object {
        const val STEP_COMPLETED = Integer.MAX_VALUE

        const val ANIMATE_TRANSITION_KEY = "animate"
        const val CENTER_X_KEY = "centerX"
        const val CENTER_Y_KEY = "centerY"

        /**
         * Intent broadcasted by pager fragments to go next
         */
        const val PAGER_NEXT_INTENT = "welcome.pager.next"

        /**
         * Intent broadcasted by pager fragments to go previous
         */
        const val PAGER_PREVIOUS_INTENT = "welcome.pager.previous"

        /**
         * Intent broadcasted by pager fragments when welcome onboarding is done
         */
        const val PAGER_DONE_INTENT = "welcome.pager.done"
    }

    /**
     *
     */
    fun loading(show: Boolean) {
        binding.loader.visibility = if (show) View.VISIBLE else View.GONE
    }
}


/**
 * The current onboarding step (int)
 */
private const val ONBOARDING_STEP_PARAMETERS_KEY = "onboarding_step"

fun AppPreferences.getOnboardingStep(): Int {
    return getInt(ONBOARDING_STEP_PARAMETERS_KEY, 0)
}

private fun AppPreferences.setOnboardingStep(step: Int) {
    putInt(ONBOARDING_STEP_PARAMETERS_KEY, step)
}
