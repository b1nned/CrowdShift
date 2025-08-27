package com.example.crowdshift

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat

class CrowdShiftJSInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun showLongToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @JavascriptInterface
    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open link")
        }
    }

    @JavascriptInterface
    fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open settings")
        }
    }

    @JavascriptInterface
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @JavascriptInterface
    fun requestLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open location settings")
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "platform": "Android",
                "version": "${android.os.Build.VERSION.RELEASE}",
                "model": "${android.os.Build.MODEL}",
                "manufacturer": "${android.os.Build.MANUFACTURER}",
                "appVersion": "${getAppVersion()}",
                "cardId": "${getCardId()}",
                "loginMethod": "${getLoginMethod()}"
            }
        """.trimIndent()
    }

    @JavascriptInterface
    fun vibrate(duration: Long = 100) {
        try {
            val vibrator = ContextCompat.getSystemService(context, android.os.Vibrator::class.java)
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Vibration not supported or permission not granted
        }
    }

    @JavascriptInterface
    fun shareContent(title: String, text: String, url: String = "") {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, if (url.isNotEmpty()) "$text\n$url" else text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            showToast("Cannot share content")
        }
    }

    @JavascriptInterface
    fun logout() {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.performLogout()
            }
        }
    }

    @JavascriptInterface
    fun refreshPage() {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.webView.reload()
            }
        }
    }

    @JavascriptInterface
    fun openQRScanner() {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.startQRScanner()
            }
        }
    }

    @JavascriptInterface
    fun goBack() {
        if (context is MainActivity) {
            context.runOnUiThread {
                if (context.webView.canGoBack()) {
                    context.webView.goBack()
                }
            }
        }
    }

    @JavascriptInterface
    fun setTitle(title: String) {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.supportActionBar?.title = title
            }
        }
    }

    @JavascriptInterface
    fun showConnectionError() {
        showLongToast("Connection error. Please check your internet connection and pull to refresh.")
    }

    @JavascriptInterface
    fun trackEvent(eventName: String, eventData: String = "") {
        // Log events for analytics (you can integrate with Firebase Analytics here)
        android.util.Log.d("CrowdShift", "Event: $eventName, Data: $eventData")
    }

    @JavascriptInterface
    fun saveToStorage(key: String, value: String) {
        try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            android.util.Log.e("CrowdShift", "Failed to save to storage", e)
        }
    }

    @JavascriptInterface
    fun getFromStorage(key: String): String? {
        return try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun clearStorage() {
        try {
            val prefs = context.getSharedPreferences("crowdshift_web_storage", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e("CrowdShift", "Failed to clear storage", e)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun getCardId(): String {
        return if (context is MainActivity) {
            context.cardId ?: ""
        } else ""
    }

    private fun getLoginMethod(): String {
        return if (context is MainActivity) {
            context.loginMethod ?: ""
        } else ""
    }
}