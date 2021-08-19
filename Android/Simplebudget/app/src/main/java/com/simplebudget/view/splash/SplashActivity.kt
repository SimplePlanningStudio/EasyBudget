package com.simplebudget.view.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.simplebudget.R
import com.simplebudget.helper.BaseActivity
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.push.MyFirebaseMessagingService.Companion.WEEKLY_REMINDER_KEY
import com.simplebudget.view.main.MainActivity
import com.simplebudget.view.welcome.WelcomeActivity
import com.simplebudget.view.welcome.getOnboardingStep
import org.koin.android.ext.android.inject

class SplashActivity : BaseActivity() {

    private val appPreferences: AppPreferences by inject()

    /**
     *
     */
    override fun onStart() {
        super.onStart()
        checkToken()
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeScreen()
    }

    private fun homeScreen() {
        if ((appPreferences.getOnboardingStep() == WelcomeActivity.STEP_COMPLETED)) {
            val mainActivityIntent = Intent(this@SplashActivity, MainActivity::class.java)
            if (intent?.hasExtra(WEEKLY_REMINDER_KEY) == true) {
                mainActivityIntent.putExtra(WEEKLY_REMINDER_KEY, true)
            }
            startActivity(mainActivityIntent)
            finish()
        } else {
            setContentView(R.layout.activity_splash)

            Handler().postDelayed({
                if (appPreferences.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
                    val startIntent = Intent(this@SplashActivity, WelcomeActivity::class.java)
                    startActivity(startIntent)
                    finish()
                } else {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }, 2500)
        }
    }

    private fun checkToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("FCM TOKEN", token ?: "")
        })
    }
}