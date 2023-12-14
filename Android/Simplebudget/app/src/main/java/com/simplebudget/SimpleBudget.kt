package com.simplebudget

import android.app.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.google.android.gms.ads.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simplebudget.db.DB
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.FirebaseAnalyticsHelper
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.injection.appModule
import com.simplebudget.injection.viewModelModule
import com.simplebudget.prefs.*
import com.simplebudget.view.RatingPopup
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.*
import com.simplebudget.view.main.MainActivity as MainActivity


/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
class SimpleBudget : MultiDexApplication(), Application.ActivityLifecycleCallbacks,
    LifecycleObserver, Configuration.Provider {

    private val appPreferences: AppPreferences by inject()
    private val db: DB by inject()
    private var activityCounter = 0
    // ------------------------------------------>

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        startKoin {
            //androidLogger()
            androidLogger(Level.ERROR)
            androidContext(this@SimpleBudget)
            modules(listOf(appModule, viewModelModule))
        }

        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false).not()) {
            initAdsSdk()
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
        db.run {
            db.ensureDBCreated()
            // FIXME this should be done on restore, change that for the whole parameters restoration
            if (appPreferences.getShouldResetInitDate()) {
                runBlocking { getOldestExpense() }?.let { expense ->
                    appPreferences.setInitDate(expense.date.toStartOfDayDate())
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
        /**
         * Initialize FirebaseAnalyticsHelper
         */
        if (ENABLE_ANALYTICS) {
            FirebaseAnalyticsHelper.initialize(FirebaseAnalytics.getInstance(this))
        }

        /*
         * Save first launch date if needed
         */
        val initDate = appPreferences.getInitDate()
        if (initDate == null) {
            appPreferences.setInitDate(Date())
            // Set a default currency before on boarding
            appPreferences.setUserCurrency(appPreferences.getUserCurrency())
        }

        /*
         * Create local ID if needed
         */
        var localId = appPreferences.getLocalId()
        if (localId == null) {
            localId = UUID.randomUUID().toString()
            appPreferences.setLocalId(localId)
        }
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
                    val shown = RatingPopup(activity, appPreferences).show(false)
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
     * Check if a an update occurred and call [.onUpdate] if so
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
     * Called when the app goes background
     */
    private fun onAppBackground() {
        Logger.debug("onAppBackground")
    }

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
     * Init Ads SDK
     */
    private fun initAdsSdk() {
        MobileAds.initialize(this) { }
    }

    /** ActivityLifecycleCallback methods. */
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

    /**
     * Provides configurations
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return if (BuildConfig.DEBUG) {
            Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build()
        } else {
            Configuration.Builder().setMinimumLoggingLevel(Log.ERROR).build()
        }
    }
}
