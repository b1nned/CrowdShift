package com.example.crowdshift

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sharedPreferences: SharedPreferences
    private var userBeepId: String? = null

    companion object {
        private const val CROWDSHIFT_URL = "https://larkaholic.github.io/Crowdshift/"
        private const val PREF_NAME = "CrowdShiftPrefs"
        private const val KEY_BEEP_ID = "beep_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        fun start(context: Context, beepId: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("beepId", beepId)
            }
            context.startActivity(intent)
        }
    }

    // Modern permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showToast("All permissions granted")
        } else {
            showToast("Some features may be limited without permissions")
        }
    }

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        handleQRScanResult(result.contents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupWebView()
        setupSwipeRefresh()
        requestPermissions()

        // Get user data from intent or preferences
        userBeepId = intent.getStringExtra("beepId") ?: sharedPreferences.getString(KEY_BEEP_ID, null)

        if (userBeepId != null) {
            saveUserSession(userBeepId!!)
            loadWebAppWithAutoLogin()
        } else {
            // Redirect to login if no valid session
            redirectToLogin()
        }
    }

    private fun initializeComponents() {
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString CrowdShiftApp/1.0"
        }

        webView.webViewClient = CrowdShiftWebViewClient()
        webView.webChromeClient = CrowdShiftWebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "CrowdShiftAndroid")
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.apply {
            setOnRefreshListener { webView.reload() }
            setColorSchemeColors(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_dark)
            )
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun saveUserSession(beepId: String) {
        sharedPreferences.edit().apply {
            putString(KEY_BEEP_ID, beepId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    private fun clearUserSession() {
        sharedPreferences.edit().apply {
            remove(KEY_BEEP_ID)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    private fun loadWebAppWithAutoLogin() {
        val urlWithAuth = "$CROWDSHIFT_URL?beepId=$userBeepId&autoLogin=true"
        webView.loadUrl(urlWithAuth)
    }

    private fun redirectToLogin() {
        clearUserSession()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun handleQRScanResult(result: String?) {
        result?.let { qrData ->
            val jsCode = "javascript:if(typeof handleQRResult === 'function') { handleQRResult('$qrData'); }"
            webView.evaluateJavascript(jsCode) {
                showToast("QR Code processed: $qrData")
            }
        } ?: run {
            val jsCode = "javascript:if(typeof handleQRCancel === 'function') { handleQRCancel(); }"
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    /**
     * JavaScript Interface for communication between web app and native Android
     */
    inner class WebAppInterface {

        @JavascriptInterface
        fun openQRScanner() {
            runOnUiThread {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                    val options = ScanOptions().apply {
                        setPrompt("Scan QR Code")
                        setBeepEnabled(true)
                        setOrientationLocked(false)
                        setBarcodeImageEnabled(true)
                    }
                    qrScannerLauncher.launch(options)
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    showToast("Camera permission required for QR scanning")
                }
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                showToast(message)
            }
        }

        @JavascriptInterface
        fun getUserId(): String {
            return userBeepId ?: ""
        }

        @JavascriptInterface
        fun logout() {
            runOnUiThread {
                clearUserSession()
                redirectToLogin()
            }
        }

        @JavascriptInterface
        fun isLoggedIn(): Boolean {
            return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        }
    }

    /**
     * Custom WebViewClient for handling navigation and page loading
     */
    inner class CrowdShiftWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request?.url?.let { uri ->
                val urlString = uri.toString()

                // Allow navigation within CrowdShift domains and local assets
                if (urlString.startsWith("file:///android_asset/") ||
                    uri.host?.contains("crowdshift", ignoreCase = true) == true ||
                    uri.host?.contains("larkaholic.github.io", ignoreCase = true) == true) {
                    return false // Let WebView handle it
                }

                // Handle external links
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            swipeRefreshLayout.isRefreshing = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false
            injectMobileOptimizations()
            injectUserSession()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            swipeRefreshLayout.isRefreshing = false

            if (request?.isForMainFrame == true) {
                showToast("Connection error. Please check your internet and try again.")
            }
        }

        private fun injectMobileOptimizations() {
            val javascript = """
                (function() {
                    // Add viewport meta tag if not present
                    if (!document.querySelector('meta[name="viewport"]')) {
                        var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, user-scalable=yes';
                        document.getElementsByTagName('head')[0].appendChild(meta);
                    }
                    
                    // Mobile-friendly styles
                    var style = document.createElement('style');
                    style.textContent = `
                        body { 
                            -webkit-text-size-adjust: 100%; 
                            -webkit-user-select: none;
                            -webkit-touch-callout: none;
                        }
                        * {
                            -webkit-tap-highlight-color: rgba(0,0,0,0);
                        }
                    `;
                    document.head.appendChild(style);
                })();
            """.trimIndent()

            webView.evaluateJavascript(javascript, null)
        }

        private fun injectUserSession() {
            userBeepId?.let { beepId ->
                val javascript = """
                    (function() {
                        // Set user session data
                        if (typeof window.CrowdShiftSession === 'undefined') {
                            window.CrowdShiftSession = {
                                userId: '$beepId',
                                isLoggedIn: true,
                                loginTime: new Date().toISOString()
                            };
                        }
                        
                        // Trigger auto-login if function exists
                        if (typeof handleAutoLogin === 'function') {
                            handleAutoLogin('$beepId');
                        }
                        
                        // Store in localStorage if available
                        if (typeof localStorage !== 'undefined') {
                            localStorage.setItem('crowdshift_user_id', '$beepId');
                            localStorage.setItem('crowdshift_logged_in', 'true');
                        }
                    })();
                """.trimIndent()

                webView.evaluateJavascript(javascript, null)
            }
        }
    }

    /**
     * Custom WebChromeClient for advanced web features
     */
    inner class CrowdShiftWebChromeClient : WebChromeClient() {

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            // Auto-grant location permission for trusted origins
            val trustedOrigins = listOf(
                "file://",
                "larkaholic.github.io",
                "crowdshift"
            )

            val isTrusted = trustedOrigins.any { origin?.contains(it, ignoreCase = true) == true }

            if (isTrusted) {
                callback?.invoke(origin, true, false)
            } else {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.let { permissionRequest ->
                when {
                    permissionRequest.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) -> {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            permissionRequest.grant(permissionRequest.resources)
                        } else {
                            permissionRequest.deny()
                            showToast("Camera permission needed")
                        }
                    }
                    else -> permissionRequest.deny()
                }
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            supportActionBar?.title = title ?: "CrowdShift"
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            // You can add a progress bar here if needed
        }
    }
}