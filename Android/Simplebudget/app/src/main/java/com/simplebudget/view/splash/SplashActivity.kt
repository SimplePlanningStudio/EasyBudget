package com.simplebudget.view.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.simplebudget.databinding.ActivitySplashBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.Rate.openPlayStore
import com.simplebudget.helper.extensions.addExtrasForDownloadCampaign
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.push.MyFirebaseMessagingService.Companion.WEEKLY_REMINDER_KEY
import com.simplebudget.view.main.MainActivity
import com.simplebudget.view.welcome.WelcomeActivity
import com.simplebudget.view.welcome.getOnboardingStep
import org.koin.android.ext.android.inject

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val appPreferences: AppPreferences by inject()

    /**
     *
     */
    override fun createBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeScreen()
    }

    /**
     *
     */
    private fun homeScreen() {
        if ((appPreferences.getOnboardingStep() == WelcomeActivity.STEP_COMPLETED)) {
            var mainActivityIntent = Intent(this@SplashActivity, MainActivity::class.java)
            mainActivityIntent = mainActivityIntent.addExtrasForDownloadCampaign(
                intent.getStringExtra("package") ?: ""
            )
            if (intent?.hasExtra(WEEKLY_REMINDER_KEY) == true) {
                mainActivityIntent.putExtra(WEEKLY_REMINDER_KEY, true)
            }
            startActivity(mainActivityIntent)
            finishAffinity()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                if (appPreferences.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
                    startActivity(
                        Intent(this@SplashActivity, WelcomeActivity::class.java)
                    )
                    finish()
                } else {
                    var mainActivityIntent = Intent(this@SplashActivity, MainActivity::class.java)
                    mainActivityIntent = mainActivityIntent.addExtrasForDownloadCampaign(
                        intent.getStringExtra("package") ?: ""
                    )
                    startActivity(mainActivityIntent)
                    finishAffinity()
                }
            }, 2500)
        }
    }

}