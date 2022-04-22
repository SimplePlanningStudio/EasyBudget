package com.simplebudget.helper

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReview {
    fun askForReview(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val requestReviewFlow = reviewManager.requestReviewFlow()
        requestReviewFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = request.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                }
            } else {
                Log.d("Error: ", request.exception.toString())
                // There was some problem, continue regardless of the result.
            }
        }
    }
}