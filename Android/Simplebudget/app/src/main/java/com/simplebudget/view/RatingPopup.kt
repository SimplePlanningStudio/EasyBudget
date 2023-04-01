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
package com.simplebudget.view

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.hasUserCompleteRating
import com.simplebudget.prefs.setUserHasCompleteRating

/**
 * Rating popup that ask user for feedback and redirect them to the PlayStore
 * Manual rating updated to In app review.
 * @author Benoit LETONDOR
 */
class RatingPopup(
    private val context: Context,
    private val appPreferences: AppPreferences
) {

    /**
     * Show the rating popup to the user
     *
     * @param forceShow force show even if the user already completed it. If not forced, the user
     * will have a button to asked not to be asked again
     * @return true if the popup has been shown, false otherwise
     */
    fun show(forceShow: Boolean): Boolean {
        if (!forceShow && appPreferences.hasUserCompleteRating()) {
            Logger.debug("Not showing rating cause user already completed it")
            return false
        }
        showNormalRatingPopup(forceShow)
        return true
    }

    /**
     *
     */
    private fun showNormalRatingPopup(forceShow: Boolean) {
        appPreferences.setRatingPopupStep(RatingPopupStep.STEP_SHOWN)
        val dialog = buildStep1(includeDontAskMeAgainButton = !forceShow)
        dialog.show()
        if (!forceShow) {
            // Center buttons
            dialog.centerButtons()
        }
    }

    /**
     * Build the first step of rating asking the user what he thinks of the app
     *
     * @param includeDontAskMeAgainButton boolean that indicates of the don't ask me again button
     * should be included
     * @return A ready to be shown [AlertDialog]
     */
    private fun buildStep1(includeDontAskMeAgainButton: Boolean): AlertDialog {
        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_question_title)
            .setMessage(R.string.rating_popup_question_message)
            .setNegativeButton(R.string.rating_popup_question_cta_negative) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_DISLIKE)
                buildNegativeStep().show()
            }
            .setPositiveButton(R.string.rating_popup_question_cta_positive) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_LIKE)
                buildPositiveStep().show()
            }

        if (includeDontAskMeAgainButton) {
            builder.setNeutralButton(R.string.rating_popup_question_cta_dont_ask_again) { dialog, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_NOT_ASK_ME_AGAIN)
                appPreferences.setUserHasCompleteRating()
                dialog.dismiss()
            }
        }

        return builder.create()
    }

    /**
     * Build the step to shown when the user said he doesn't like the app
     *
     * @return A ready to be shown [AlertDialog]
     */
    private fun buildNegativeStep(): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_negative_title)
            .setMessage(R.string.rating_popup_negative_message)
            .setNegativeButton(R.string.no_thanks) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_DISLIKE_NO_FEEDBACK)
                appPreferences.setUserHasCompleteRating()
            }
            .setPositiveButton(R.string.sure) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_DISLIKE_FEEDBACK)
                appPreferences.setUserHasCompleteRating()
                Feedback.askForFeedback(context)
            }
            .create()
    }

    /**
     * Build the step to shown when the user said he likes the app
     *
     * @return A ready to be shown [AlertDialog]
     */
    private fun buildPositiveStep(): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_positive_title)
            .setMessage(R.string.rating_popup_positive_message)
            .setNegativeButton(R.string.no_thanks) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_LIKE_NOT_RATED)
                appPreferences.setUserHasCompleteRating()
            }
            .setPositiveButton(R.string.sure) { _, _ ->
                appPreferences.setRatingPopupStep(RatingPopupStep.STEP_LIKE_RATED)
                appPreferences.setUserHasCompleteRating()
                Rate.onPlayStore(context)
            }
            .create()
    }

    /**
     * An enum that define the user step in the rating process
     */
    enum class RatingPopupStep(val value: Int) {
        /**
         * Rating popup not shown
         */
        STEP_NOT_SHOWN(0),

        /**
         * Rating popup shown
         */
        STEP_SHOWN(1),

        /**
         * User clicked "I like the app"
         */
        STEP_LIKE(2),

        /**
         * User clicked "I don't want to rate the app"
         */
        STEP_LIKE_NOT_RATED(3),

        /**
         * User clicked "I want to rate the app"
         */
        STEP_LIKE_RATED(4),

        /**
         * User clicked "I don't like the app"
         */
        STEP_DISLIKE(5),

        /**
         * User clicked "I don't wanna give my feedback"
         */
        STEP_DISLIKE_NO_FEEDBACK(6),

        /**
         * User clicked "I want to give my feedback"
         */
        STEP_DISLIKE_FEEDBACK(7),

        /**
         * User clicked "Don't ask me again" on the first screen
         */
        STEP_NOT_ASK_ME_AGAIN(8);


        companion object {

            /**
             * Retrive the enum for the given value
             *
             * @param value value to find
             * @return the enum if found, null otherwise
             */
            fun fromValue(value: Int): RatingPopupStep? {
                for (step in values()) {
                    if (step.value == value) {
                        return step
                    }
                }

                return null
            }
        }
    }
}

/**
 * Store the user step in the rating process (int)
 */
private const val RATING_STEP_PARAMETERS_KEY = "rating_step"

/**
 * Get the current user rating step
 */
fun AppPreferences.getRatingPopupUserStep(): RatingPopup.RatingPopupStep? {
    return RatingPopup.RatingPopupStep.fromValue(
        getInt(
            RATING_STEP_PARAMETERS_KEY,
            RatingPopup.RatingPopupStep.STEP_NOT_SHOWN.value
        )
    )
}

/**
 * Set the current user rating step
 */
private fun AppPreferences.setRatingPopupStep(step: RatingPopup.RatingPopupStep) {
    putInt(RATING_STEP_PARAMETERS_KEY, step.value)
}
