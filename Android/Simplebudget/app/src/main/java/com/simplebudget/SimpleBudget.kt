package com.simplebudget

import android.app.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simplebudget.db.DB
import com.simplebudget.helper.*
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

    private var appOpenAdManager: AppOpenAdManager? = null
    private var currentActivity: Activity? = null
    private val logTag = "SimpleBudgetApplication"
    private var activityCounter = 0
    private var appOpenAdsDisplayCount = 0
    private var maxAppOpenAdsDisplayCount = 2

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
            appOpenAdManager = AppOpenAdManager()
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

    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false).not()) {
            currentActivity?.let { appOpenAdManager?.showAdIfAvailable(it) }
        }
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activityCounter == 0) {
            onAppForeground(activity)
        }
        activityCounter++
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        appOpenAdManager?.let {
            if (!it.isShowingAd) {
                currentActivity = activity
            }
        }
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
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false).not()) {
            appOpenAdManager?.showAdIfAvailable(activity, onShowAdCompleteListener)
        }
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** Inner class that loads and shows app open ads. */
    private inner class AppOpenAdManager {

        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }
            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                context.getString(R.string.app_open_ad_unit),
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(logTag, "onAdLoaded.")
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.d(logTag, "onAdFailedToLoad: " + loadAdError.message)
                    }
                }
            )
        }

        /** Check if ad was loaded more than n hours ago. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            // Ad references in the app open beta will time out after four hours, but this time limit
            // may change in future beta versions. For details, see:
            // https://support.google.com/admob/answer/9341964?hl=en
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                }
            )
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) return
            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }
            Log.d(logTag, "Will show ad.")
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(logTag, "onAdDismissedFullScreenContent.")
                    onShowAdCompleteListener.onShowAdComplete()
                    if (appOpenAdsDisplayCount < maxAppOpenAdsDisplayCount)
                        loadAd(activity)
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(logTag, "onAdFailedToShowFullScreenContent: " + adError.message)

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                /** Called when fullscreen content is shown. */
                override fun onAdShowedFullScreenContent() {
                    appOpenAdsDisplayCount++
                    Log.d(logTag, "onAdShowedFullScreenContent.")
                }
            }
            isShowingAd = true
            if (appOpenAdsDisplayCount < maxAppOpenAdsDisplayCount)
                appOpenAd?.show(activity)
        }
    }

    /**
     * Provides configurations
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return if (BuildConfig.DEBUG) {
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        } else {
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.ERROR)
                .build()
        }
    }
}
