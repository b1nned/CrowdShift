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
import androidx.appcompat.app.AlertDialog

class CrowdShiftJSInterface(private val context: Context) {

    private val mainActivity: MainActivity?
        get() = if (context is MainActivity) context else null

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
            Log.d(TAG, "Toast shown: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast", e)
        }
    }

    @JavascriptInterface
    fun showLongToast(message: String) {
        try {
            mainHandler.post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            Log.d(TAG, "Long toast shown: $message")
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
            Log.d(TAG, "External URL opened: $url")
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
            Log.d(TAG, "App settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            showToast("Cannot open settings")
        }
    }

    @JavascriptInterface
    fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            Log.d(TAG, "Location enabled: $isEnabled")
            isEnabled
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
            Log.d(TAG, "Location settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening location settings", e)
            showToast("Cannot open location settings")
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            val displayMetrics = context.resources.displayMetrics
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
                "screenWidth": ${displayMetrics.widthPixels},
                "screenHeight": ${displayMetrics.heightPixels},
                "density": ${displayMetrics.density},
                "scaledDensity": ${displayMetrics.scaledDensity},
                "timestamp": ${System.currentTimeMillis()}
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
            mainHandler.post {
                val vibrator = ContextCompat.getSystemService(context, android.os.Vibrator::class.java)
                val safeDuration = duration.coerceIn(10, 1000)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            safeDuration,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(safeDuration)
                }

                Log.d(TAG, "Vibration triggered: ${safeDuration}ms")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration not available", e)
        }
    }

    @JavascriptInterface
    fun shareContent(title: String, text: String, url: String = "") {
        try {
            mainHandler.post {
                val shareText = if (url.isNotEmpty()) "$text\n$url" else text
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    val chooser = Intent.createChooser(shareIntent, "Share via")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    context.startActivity(shareIntent)
                }

                Log.d(TAG, "Content shared: $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing content", e)
            showToast("Cannot share content")
        }
    }

    @JavascriptInterface
    fun logout() {
        try {
            Log.d(TAG, "Logout requested from web")
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
            Log.d(TAG, "Page refresh requested")
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
            Log.d(TAG, "QR scanner requested")
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
            Log.d(TAG, "Go back requested")
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
            Log.d(TAG, "Title change requested: $title")
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
            Log.i("CrowdShiftAnalytics", "Event: $eventName, Data: $eventData")
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
            Log.d(TAG, "Retrieved from storage: $key")
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
            mainHandler.post {
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveButton, null)
                    .setCancelable(true)
                    .show()
            }
            Log.d(TAG, "Native dialog shown: $title")
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
            Log.d(TAG, "Map opened: $latitude, $longitude")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening map", e)
            showToast("Cannot open map")
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            mainHandler.post {
                val clipboard = ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("CrowdShift", text)
                clipboard?.setPrimaryClip(clip)
                showToast("Copied to clipboard")
            }
            Log.d(TAG, "Text copied to clipboard")
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

    @JavascriptInterface
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = ContextCompat.getSystemService(context, android.net.ConnectivityManager::class.java)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager?.activeNetwork
                val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
                networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager?.activeNetworkInfo
                activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            false
        }
    }

    @JavascriptInterface
    fun getBatteryLevel(): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val batteryManager = ContextCompat.getSystemService(context, android.os.BatteryManager::class.java)
                batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            -1
        }
    }

    @JavascriptInterface
    fun openDialer(phoneNumber: String = "") {
        try {
            val uri = if (phoneNumber.isNotEmpty()) "tel:$phoneNumber" else "tel:"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Dialer opened: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening dialer", e)
            showToast("Cannot open dialer")
        }
    }

    @JavascriptInterface
    fun sendEmail(to: String = "", subject: String = "", body: String = "") {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$to")
                if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Send email"))
            Log.d(TAG, "Email intent opened: $to")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening email", e)
            showToast("Cannot open email")
        }
    }

    @JavascriptInterface
    fun getStorageKeys(): String {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            val keys = prefs.all.keys.toList()
            val jsonKeys = keys.joinToString("\", \"", "[\"", "\"]")
            Log.d(TAG, "Storage keys retrieved: ${keys.size} keys")
            jsonKeys
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage keys", e)
            "[]"
        }
    }

    @JavascriptInterface
    fun logDebug(message: String) {
        Log.d("WebView", message)
    }

    @JavascriptInterface
    fun logError(message: String) {
        Log.e("WebView", message)
    }

    @JavascriptInterface
    fun notifyFeatureUnavailable(featureName: String) {
        showToast("$featureName is not available in this version")
        Log.w(TAG, "Feature unavailable: $featureName")
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