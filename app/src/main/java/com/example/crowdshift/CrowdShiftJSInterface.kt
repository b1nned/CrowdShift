package com.example.crowdshift

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.util.Log

class CrowdShiftJSInterface(private val context: Context) {

    private val mainActivity: MainActivity? get() = if (context is MainActivity) context else null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "CrowdShiftJS"
    }

    @JavascriptInterface
    fun showToast(message: String) {
        try {
            mainHandler.post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast", e)
        }
    }

    @JavascriptInterface
    fun showLongToast(message: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing long toast", e)
        }
    }

    @JavascriptInterface
    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening external URL: $url", e)
            showToast("Cannot open link")
        }
    }

    @JavascriptInterface
    fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            showToast("Cannot open settings")
        }
    }

    @JavascriptInterface
    fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location status", e)
            false
        }
    }

    @JavascriptInterface
    fun requestLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening location settings", e)
            showToast("Cannot open location settings")
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            """
            {
                "platform": "Android",
                "version": "${android.os.Build.VERSION.RELEASE}",
                "model": "${android.os.Build.MODEL}",
                "manufacturer": "${android.os.Build.MANUFACTURER}",
                "appVersion": "${getAppVersion()}",
                "cardId": "${getCardId()}",
                "loginMethod": "${getLoginMethod()}",
                "isNativeApp": true,
                "screenWidth": ${context.resources.displayMetrics.widthPixels},
                "screenHeight": ${context.resources.displayMetrics.heightPixels},
                "density": ${context.resources.displayMetrics.density}
            }
            """.trimIndent()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info", e)
            """{"platform": "Android", "error": "Unable to get device info"}"""
        }
    }

    @JavascriptInterface
    fun vibrate(duration: Long = 100) {
        try {
            Handler(Looper.getMainLooper()).post {
                val vibrator = ContextCompat.getSystemService(context, android.os.Vibrator::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            duration.coerceIn(10, 1000), // Limit duration between 10ms and 1s
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(duration.coerceIn(10, 1000))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration not available", e)
        }
    }

    @JavascriptInterface
    fun shareContent(title: String, text: String, url: String = "") {
        try {
            Handler(Looper.getMainLooper()).post {
                val shareText = if (url.isNotEmpty()) "$text\n$url" else text
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                } catch (e: Exception) {
                    context.startActivity(shareIntent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing content", e)
            showToast("Cannot share content")
        }
    }

    @JavascriptInterface
    fun logout() {
        try {
            val activity = mainActivity
            activity?.runOnUiThread {
                activity.performLogout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            showToast("Error during logout")
        }
    }

    @JavascriptInterface
    fun refreshPage() {
        try {
            val activity = mainActivity
            activity?.runOnUiThread {
                activity.webView.reload()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing page", e)
            showToast("Cannot refresh page")
        }
    }

    @JavascriptInterface
    fun openQRScanner() {
        try {
            val activity = mainActivity
            activity?.runOnUiThread {
                activity.startQRScanner()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening QR scanner", e)
            showToast("Cannot open QR scanner")
        }
    }

    @JavascriptInterface
    fun goBack() {
        try {
            val activity = mainActivity
            activity?.runOnUiThread {
                if (activity.webView.canGoBack()) {
                    activity.webView.goBack()
                } else {
                    showToast("Cannot go back")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error going back", e)
            showToast("Cannot go back")
        }
    }

    @JavascriptInterface
    fun setTitle(title: String) {
        try {
            val activity = mainActivity
            activity?.runOnUiThread {
                activity.supportActionBar?.title = title
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting title", e)
        }
    }

    @JavascriptInterface
    fun showConnectionError() {
        showLongToast("Connection error. Please check your internet connection and pull to refresh.")
    }

    @JavascriptInterface
    fun trackEvent(eventName: String, eventData: String = "") {
        try {
            // Log events for analytics (integrate with Firebase Analytics here if needed)
            Log.d("CrowdShiftAnalytics", "Event: $eventName, Data: $eventData")

            // You can add more sophisticated analytics tracking here
            val eventInfo = """
                Event: $eventName
                Data: $eventData
                Timestamp: ${System.currentTimeMillis()}
                User: ${getCardId()}
            """.trimIndent()

            Log.i(TAG, "Analytics: $eventInfo")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking event", e)
        }
    }

    @JavascriptInterface
    fun saveToStorage(key: String, value: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            Log.d(TAG, "Saved to storage: $key")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to storage: $key", e)
            false
        }
    }

    @JavascriptInterface
    fun getFromStorage(key: String): String? {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            val value = prefs.getString(key, null)
            Log.d(TAG, "Retrieved from storage: $key = ${if (value != null) "***" else "null"}")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get from storage: $key", e)
            null
        }
    }

    @JavascriptInterface
    fun clearStorage(): Boolean {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Cleared web storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear storage", e)
            false
        }
    }

    @JavascriptInterface
    fun hasStorageKey(key: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.contains(key)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check storage key: $key", e)
            false
        }
    }

    @JavascriptInterface
    fun removeFromStorage(key: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.edit().remove(key).apply()
            Log.d(TAG, "Removed from storage: $key")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from storage: $key", e)
            false
        }
    }

    @JavascriptInterface
    fun showNativeDialog(title: String, message: String, positiveButton: String = "OK") {
        try {
            mainActivity?.runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveButton, null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing native dialog", e)
            showToast(message)
        }
    }

    @JavascriptInterface
    fun openMap(latitude: Double, longitude: Double, label: String = "") {
        try {
            val geoUri = if (label.isNotEmpty()) {
                "geo:$latitude,$longitude?q=$latitude,$longitude($label)"
            } else {
                "geo:$latitude,$longitude"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening map", e)
            showToast("Cannot open map")
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                val clipboard = ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("CrowdShift", text)
                clipboard?.setPrimaryClip(clip)
                showToast("Copied to clipboard")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard", e)
            showToast("Cannot copy to clipboard")
        }
    }

    @JavascriptInterface
    fun getNetworkType(): String {
        return try {
            val connectivityManager = ContextCompat.getSystemService(context, android.net.ConnectivityManager::class.java)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager?.activeNetwork
                val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)

                when {
                    networkCapabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
                    networkCapabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cellular"
                    networkCapabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ethernet"
                    else -> "unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager?.activeNetworkInfo
                when (activeNetworkInfo?.type) {
                    android.net.ConnectivityManager.TYPE_WIFI -> "wifi"
                    android.net.ConnectivityManager.TYPE_MOBILE -> "cellular"
                    android.net.ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                    else -> "unknown"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            "unknown"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version", e)
            "1.0"
        }
    }

    private fun getCardId(): String {
        return mainActivity?.cardId ?: ""
    }

    private fun getLoginMethod(): String {
        return mainActivity?.loginMethod ?: "unknown"
    }
}