package com.example.crowdshift

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.example.crowdshift.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CrowdShiftApplication : Application() {

    // Application-wide coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Shared preferences
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "crowdshift_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_WEB_URL = "web_url"
        private const val KEY_USER_AGENT = "user_agent"

        @Volatile
        private var INSTANCE: CrowdShiftApplication? = null

        fun getInstance(): CrowdShiftApplication {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: throw IllegalStateException("Application not initialized")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        initializeApp()
    }

    private fun initializeApp() {
        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set default night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Initialize WebView early (improves first load performance)
        try {
            WebView(this)
        } catch (e: Exception) {
            android.util.Log.e("CrowdShift", "Failed to initialize WebView", e)
        }

        // Set up crash reporting (if using a service like Crashlytics)
        setupCrashReporting()

        // Preload essential data
        preloadData()
    }

    private fun setupCrashReporting() {
        // Set up global exception handler for unhandled exceptions
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("CrowdShift", "Uncaught exception in thread ${thread.name}", exception)

            // Save crash info to preferences or log file
            saveCrashInfo(exception)

            // Call the default handler
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
    }

    private fun preloadData() {
        // Preload any essential data or configurations
        // This can include user preferences, cached data, etc.
    }

    private fun saveCrashInfo(exception: Throwable) {
        try {
            val crashInfo = buildString {
                appendLine("Timestamp: ${System.currentTimeMillis()}")
                appendLine("App Version: ${BuildConfig.VERSION_NAME}")
                appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
                appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Exception: ${exception.message}")
                appendLine("Stack Trace:")
                exception.stackTrace.forEach { element ->
                    appendLine("  at $element")
                }
            }

            sharedPreferences.edit()
                .putString("last_crash", crashInfo)
                .putLong("last_crash_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Ignore errors in crash reporting
        }
    }

    // Utility methods for app-wide use
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }

    fun getWebUrl(): String {
        return sharedPreferences.getString(KEY_WEB_URL, BuildConfig.WEB_URL) ?: BuildConfig.WEB_URL
    }

    fun setWebUrl(url: String) {
        sharedPreferences.edit()
            .putString(KEY_WEB_URL, url)
            .apply()
    }

    fun getUserAgent(): String {
        return sharedPreferences.getString(KEY_USER_AGENT, getDefaultUserAgent()) ?: getDefaultUserAgent()
    }

    private fun getDefaultUserAgent(): String {
        return "CrowdShift/${BuildConfig.VERSION_NAME} (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
    }

    fun clearCache() {
        try {
            cacheDir.deleteRecursively()
            WebView(this).clearCache(true)
        } catch (e: Exception) {
            android.util.Log.e("CrowdShift", "Failed to clear cache", e)
        }
    }

    fun getAppVersion(): String = BuildConfig.VERSION_NAME
    fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG
}