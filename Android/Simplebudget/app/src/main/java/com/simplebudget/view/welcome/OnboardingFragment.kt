package com.simplebudget.view.welcome

import android.content.Intent
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.View

import com.simplebudget.db.DB
import org.koin.android.ext.android.inject

/**
 * Abstract fragment that contains common methods of all onboarding fragments
 *
 * @author Benoit LETONDOR
 */
abstract class OnboardingFragment : Fragment() {

    protected val db: DB by inject()

    override fun onDestroy() {
        db.close()

        super.onDestroy()
    }

    /**
     * Get the status bar color that should be used for this fragment
     *
     * @return the wanted color of the status bar
     */
    @get:ColorRes
    abstract val statusBarColor: Int

    /**
     * Go to the next onboarding step without animation
     */
    protected operator fun next() {
        val intent = Intent(WelcomeActivity.PAGER_NEXT_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    /**
     * Go to the next onboarding step with a reveal animation starting from the given center
     *
     * @param animationCenter center of the reveal animation
     */
    protected fun next(animationCenter: View) {
        val intent = Intent(WelcomeActivity.PAGER_NEXT_INTENT)
        intent.putExtra(WelcomeActivity.ANIMATE_TRANSITION_KEY, true)
        intent.putExtra(WelcomeActivity.CENTER_X_KEY, animationCenter.x.toInt() + animationCenter.width / 2)
        intent.putExtra(WelcomeActivity.CENTER_Y_KEY, animationCenter.y.toInt() + animationCenter.height / 2)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    /**
     * Finish the onboarding flow
     */
    protected fun done() {
        val intent = Intent(WelcomeActivity.PAGER_DONE_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }
}
