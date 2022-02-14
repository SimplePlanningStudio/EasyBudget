package com.simplebudget

import android.annotation.SuppressLint
import android.app.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simplebudget.appOpenAds.AppOpenManager
import com.simplebudget.db.DB
import com.simplebudget.helper.*
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.injection.appModule
import com.simplebudget.injection.viewModelModule
import com.simplebudget.notif.*
import com.simplebudget.prefs.*
import com.simplebudget.view.RatingPopup
import com.simplebudget.view.main.MainActivity
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.get
import java.util.*


/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
class SimpleBudget : Application() {

    private val appPreferences: AppPreferences by inject()

// ------------------------------------------>

    companion object {
        @SuppressLint("StaticFieldLeak")
        public var appOpenManager: AppOpenManager? = null
    }


    override fun onCreate() {
        super.onCreate()

        startKoin {
            //androidLogger()
            androidLogger(Level.ERROR)
            androidContext(this@SimpleBudget)
            modules(listOf(appModule, viewModelModule))
        }

        if (!appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            initAdsSdk()
            appOpenManager = AppOpenManager(this)
        }

        // Init actions
        init()

        // Crashlytics
        if (BuildConfig.CRASHLYTICS_ACTIVATED) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

            appPreferences.getLocalId()?.let {
                FirebaseCrashlytics.getInstance().setUserId(it)
            }
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }

        // Check if an update occurred and perform action if needed
        checkUpdateAction()

        // Ensure DB is created and reset init date if needed
        get(DB::class.java).use {
            it.ensureDBCreated()

            // FIX ME this should be done on restore, change that for the whole parameters restoration
            if (appPreferences.getShouldResetInitDate()) {
                runBlocking { it.getOldestExpense() }?.let { expense ->
                    appPreferences.setInitTimestamp(expense.date.time)
                }

                appPreferences.setShouldResetInitDate(false)
            }
        }

        // Setup theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    /**
     * Init app const and parameters
     *
     * DO NOT USE LOGGER HERE
     */
    private fun init() {
        /*
         * Save first launch date if needed
         */
        val initDate = appPreferences.getInitTimestamp()
        if (initDate <= 0) {
            appPreferences.setInitTimestamp(Date().time)
            appPreferences.setUserCurrency(Currency.getInstance(Locale.getDefault())) // Set a default currency before onboarding
        }

        /*
         * Create local ID if needed
         */
        var localId = appPreferences.getLocalId()
        if (localId == null) {
            localId = UUID.randomUUID().toString()
            appPreferences.setLocalId(localId)
        }

        // Activity counter for app foreground & background
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activityCounter = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (activityCounter == 0) {
                    onAppForeground(activity)
                }

                activityCounter++
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (activityCounter == 1) {
                    onAppBackground()
                }

                activityCounter--
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Show the rating popup if the user didn't asked not to every day after the app has been open
     * in 3 different days.
     */
    private fun showRatingPopupIfNeeded(activity: Activity) {
        try {
            if (activity !is MainActivity) {
                Logger.debug("Not showing rating popup cause app is not opened by the MainActivity")
                return
            }
            val dailyOpens = appPreferences.getNumberOfDailyOpen()
            if (dailyOpens > 2) {
                if (!hasRatingPopupBeenShownToday()) {
                    val shown = RatingPopup(activity, appPreferences).show(false, false)
                    if (shown) {
                        appPreferences.setRatingPopupLastAutoShowTimestamp(Date().time)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("Error while showing rating popup", e)
        }

    }

    /**
     * Has the rating popup been shown automatically today
     *
     * @return true if the rating popup has been shown today, false otherwise
     */
    private fun hasRatingPopupBeenShownToday(): Boolean {
        val lastRatingTS = appPreferences.getRatingPopupLastAutoShowTimestamp()
        if (lastRatingTS > 0) {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = Date(lastRatingTS)
            val lastTimeDay = cal.get(Calendar.DAY_OF_YEAR)

            return currentDay == lastTimeDay
        }

        return false
    }

    /**
     * Check if a an update occured and call [.onUpdate] if so
     */
    private fun checkUpdateAction() {
        val savedVersion = appPreferences.getCurrentAppVersion()
        if (savedVersion > 0 && savedVersion != BuildConfig.VERSION_CODE) {
            onUpdate(savedVersion, BuildConfig.VERSION_CODE)
        }

        appPreferences.setCurrentAppVersion(BuildConfig.VERSION_CODE)
    }

    /**
     * Called when an update occurred
     */
    private fun onUpdate(previousVersion: Int, @Suppress("SameParameterValue") newVersion: Int) {
        Logger.debug("Update detected, from $previousVersion to $newVersion")
    }

// -------------------------------------->

    /**
     * Called when the app goes foreground
     *
     * @param activity The activity that gone foreground
     */
    private fun onAppForeground(activity: Activity) {
        Logger.debug("onAppForeground")

        /*
         * Increment the number of open
         */
        appPreferences.setNumberOfOpen(appPreferences.getNumberOfOpen() + 1)

        /*
         * Check if last open is from another day
         */
        var shouldIncrementDailyOpen = false

        val lastOpen = appPreferences.getLastOpenTimestamp()
        if (lastOpen > 0) {
            val cal = Calendar.getInstance()
            cal.time = Date(lastOpen)

            val lastDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = Date()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            if (lastDay != currentDay) {
                shouldIncrementDailyOpen = true
            }
        } else {
            shouldIncrementDailyOpen = true
        }

        // Increment daily open
        if (shouldIncrementDailyOpen) {
            appPreferences.setNumberOfDailyOpen(appPreferences.getNumberOfDailyOpen() + 1)
        }

        /*
         * Save last open date
         */
        appPreferences.setLastOpenTimestamp(Date().time)

        /*
         * Rating popup every day after 3 opens
         */
        showRatingPopupIfNeeded(activity)
    }

    /**
     * Called when the app goes background
     */
    private fun onAppBackground() {
        Logger.debug("onAppBackground")
    }

    private fun initAdsSdk() {
        MobileAds.initialize(this) { }
    }
}
