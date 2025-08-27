package com.example.crowdshift

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var floatingNav: LinearLayout
    private lateinit var toggleNavButton: FloatingActionButton
    private lateinit var outsideOverlay: View

    private val webViewUrl = BuildConfig.WEB_URL
    var cardId: String? = null
    var loginMethod: String? = null
    private var isNavVisible = false
    private var autoHideHandler: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Auto logout timer
    private val autoLogoutHandler = Handler(Looper.getMainLooper())
    private val autoLogoutDelay = 30 * 60 * 1000L // 30 minutes
    private var autoLogoutRunnable: Runnable? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Some permissions were denied. App functionality may be limited.", Toast.LENGTH_LONG).show()
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleQRCodeResult(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "onCreate started")

            setContentView(R.layout.activity_main)

            // Get login data
            cardId = intent.getStringExtra("CARD_ID")
            loginMethod = intent.getStringExtra("LOGIN_METHOD")
            Log.d("MainActivity", "Login data retrieved: cardId=$cardId, method=$loginMethod")

            Log.d("MainActivity", "Initializing views...")
            initViews()

            Log.d("MainActivity", "Setting up toolbar...")
            setupToolbar()

            Log.d("MainActivity", "Setting up navigation...")
            setupFloatingNavigation()

            Log.d("MainActivity", "Setting up webview...")
            setupWebView()

            Log.d("MainActivity", "Checking permissions...")
            checkPermissions()

            Log.d("MainActivity", "Handling shortcuts...")
            handleShortcuts()

            Log.d("MainActivity", "Loading web app...")
            loadWebApp()

            Log.d("MainActivity", "Starting auto logout timer...")
            startAutoLogoutTimer()

            Log.d("MainActivity", "onCreate completed successfully")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            e.printStackTrace()

            // Show error dialog
            AlertDialog.Builder(this)
                .setTitle("Initialization Error")
                .setMessage("Failed to initialize the app: ${e.message}")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        toolbar = findViewById(R.id.toolbar)
        floatingNav = findViewById(R.id.floating_nav)
        toggleNavButton = findViewById(R.id.toggle_nav_button)

        // Tap outside closes nav
        outsideOverlay = findViewById(R.id.outside_overlay)
        outsideOverlay.setOnClickListener {
            hideNavigationAfterClick()
        }

        // Start hidden
        floatingNav.visibility = View.GONE
        isNavVisible = false

        toggleNavButton.setOnClickListener {
            toggleFloatingNavigation()
        }

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.secondary_blue),
            ContextCompat.getColor(this, R.color.accent_orange)
        )
    }

    private fun setupToolbar() {
        // Properly setup the toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            title = "CROWDSHIFT"
            // Make sure toolbar is visible
            show()
        }
    }

    private fun toggleFloatingNavigation() {
        if (isNavVisible) {
            hideNavigationAfterClick()
        } else {
            showNavigation()
        }

        // Rotate FAB
        val targetRotation = if (isNavVisible) 0f else 180f
        ObjectAnimator.ofFloat(toggleNavButton, "rotation", toggleNavButton.rotation, targetRotation).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupFloatingNavigation() {
        // Setup click listeners for floating navigation buttons
        findViewById<ImageButton>(R.id.nav_dashboard).setOnClickListener {
            webView.loadUrl("https://larkaholic.github.io/Crowdshift")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_city_status).setOnClickListener {
            loadCityStatus()
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_scan_qr).setOnClickListener {
            startQRScanner()
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_top_up).setOnClickListener {
            webView.loadUrl("$webViewUrl#/topup")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_shuttle_routes).setOnClickListener {
            webView.loadUrl("$webViewUrl#/shuttle")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_find_parking).setOnClickListener {
            webView.loadUrl("$webViewUrl#/parking")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_tourist_spots).setOnClickListener {
            webView.loadUrl("$webViewUrl#/tourist-spots")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_settings).setOnClickListener {
            webView.loadUrl("$webViewUrl#/settings")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_help).setOnClickListener {
            webView.loadUrl("$webViewUrl#/help")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_about).setOnClickListener {
            webView.loadUrl("$webViewUrl#/about")
            hideNavigationAfterClick()
        }

        findViewById<ImageButton>(R.id.nav_logout).setOnClickListener {
            showLogoutConfirmDialog()
            hideNavigationAfterClick()
        }
    }

    private fun showNavigation() {
        floatingNav.visibility = View.VISIBLE
        outsideOverlay.visibility = View.VISIBLE
        outsideOverlay.isClickable = true
        outsideOverlay.isFocusable = true

        // Fade in overlay
        outsideOverlay.alpha = 0f
        outsideOverlay.animate().alpha(1f).setDuration(300).start()

        ObjectAnimator.ofFloat(floatingNav, "translationX", -floatingNav.width.toFloat(), 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(floatingNav, "alpha", 0f, 1f).apply {
            duration = 300
            start()
        }
        isNavVisible = true
    }

    private fun hideNavigationAfterClick() {
        if (isNavVisible) {
            // Fade out overlay
            outsideOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                outsideOverlay.visibility = View.GONE
                outsideOverlay.isClickable = false
                outsideOverlay.isFocusable = false
            }.start()

            ObjectAnimator.ofFloat(floatingNav, "translationX", 0f, -floatingNav.width.toFloat()).apply {
                duration = 300
                interpolator = AccelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(floatingNav, "alpha", 1f, 0f).apply {
                duration = 300
                start()
            }

            floatingNav.postDelayed({
                floatingNav.visibility = View.GONE
            }, 300)

            isNavVisible = false
        }
    }

    private fun setupWebView() {
        webView.apply {
            webViewClient = CrowdShiftWebViewClient()
            webChromeClient = CrowdShiftWebChromeClient()

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // Enable location services
                setGeolocationEnabled(true)

                // Enable camera and microphone
                mediaPlaybackRequiresUserGesture = false

                // Improve text rendering
                textZoom = 100
                defaultFontSize = 16
                defaultFixedFontSize = 13

                // Enable smooth scrolling
                setRenderPriority(WebSettings.RenderPriority.HIGH)

                // Set custom user agent
                userAgentString = "$userAgentString CrowdShift/1.0 Mobile"
            }

            // Add JavaScript interface for native functionality
            addJavascriptInterface(CrowdShiftJSInterface(this@MainActivity), "CrowdShiftAndroid")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handleShortcuts() {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.data
                data?.let { uri ->
                    when (uri.scheme) {
                        "crowdshift" -> {
                            when (uri.host) {
                                "qr-scan" -> startQRScanner()
                                "city-status" -> loadCityStatus()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadWebApp() {
        val url = if (cardId != null) {
            "$webViewUrl?cardId=$cardId&method=$loginMethod"
        } else {
            webViewUrl
        }
        webView.loadUrl(url)
    }

    private fun loadCityStatus() {
        webView.loadUrl("$webViewUrl#/city-status")
    }

    fun startQRScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan QR code for quick actions")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }

        barcodeLauncher.launch(options)
    }

    private fun handleQRCodeResult(qrContent: String) {
        resetAutoLogoutTimer()

        when {
            qrContent.startsWith("crowdshift://") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrContent))
                startActivity(intent)
            }
            qrContent.contains("topup") -> {
                webView.loadUrl("$webViewUrl#/topup?qr=$qrContent")
            }
            qrContent.contains("parking") -> {
                webView.loadUrl("$webViewUrl#/parking?qr=$qrContent")
            }
            else -> {
                webView.evaluateJavascript("handleQRCode('$qrContent')", null)
            }
        }

        Toast.makeText(this, "QR Code processed successfully", Toast.LENGTH_SHORT).show()
    }

    private fun startAutoLogoutTimer() {
        autoLogoutRunnable = Runnable {
            showAutoLogoutDialog()
        }
        autoLogoutHandler.postDelayed(autoLogoutRunnable!!, autoLogoutDelay)
    }

    private fun resetAutoLogoutTimer() {
        autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }
        startAutoLogoutTimer()
    }

    private fun showAutoLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Session Timeout")
            .setMessage("You've been inactive for 30 minutes. Do you want to continue your session?")
            .setPositiveButton("Continue") { _, _ ->
                resetAutoLogoutTimer()
            }
            .setNegativeButton("Logout") { _, _ ->
                performLogout()
            }
            .setCancelable(false)
            .show()
    }

    fun performLogout() {
        // First, try to logout from the web application
        webView.evaluateJavascript("""
            javascript:(function() {
                // Try to call web app logout function if it exists
                if (typeof window.logout === 'function') {
                    window.logout();
                } else if (typeof window.signOut === 'function') {
                    window.signOut();
                } else if (typeof window.clearSession === 'function') {
                    window.clearSession();
                }
                
                // Clear web storage
                try {
                    localStorage.clear();
                    sessionStorage.clear();
                } catch(e) {
                    console.log('Could not clear storage: ' + e);
                }
                
                // Clear cookies
                document.cookie.split(";").forEach(function(c) { 
                    document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"); 
                });
                
                return 'logout_completed';
            })()
        """.trimIndent()) { result ->
            Log.d("MainActivity", "Web logout result: $result")
        }

        // Clear WebView data
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        // Clear cookies and storage
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else {
            @Suppress("DEPRECATION")
            CookieManager.getInstance().removeAllCookie()
        }

        // Clear WebStorage
        WebStorage.getInstance().deleteAllData()

        // Clear application cache if available
        try {
            // CrowdShiftApplication.getInstance()?.clearCache()
        } catch (e: Exception) {
            Log.w("MainActivity", "CrowdShiftApplication not available: ${e.message}")
        }

        // Clear auto-logout timers
        autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }
        autoHideHandler?.let { mainHandler.removeCallbacks(it) }

        // Navigate back to LoginActivity
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Clear any stored login data
            putExtra("CLEAR_SAVED_LOGIN", true)
        }

        startActivity(intent)
        finish()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Yes, Logout") { _, _ ->
                // Show loading while logging out
                progressBar.visibility = View.VISIBLE
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    inner class CrowdShiftWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
            resetAutoLogoutTimer()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false

            // Inject mobile optimization CSS and center content
            view?.evaluateJavascript("""
                javascript:(function() {
                    var meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                    document.getElementsByTagName('head')[0].appendChild(meta);
                    
                    var style = document.createElement('style');
                    style.innerHTML = `
                        * { 
                            -webkit-tap-highlight-color: transparent !important;
                            box-sizing: border-box !important;
                        }
                        html, body { 
                            -webkit-touch-callout: none;
                            -webkit-user-select: none;
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            padding: 0 !important;
                            margin: 0 !important;
                            width: 100% !important;
                            height: 100% !important;
                            overflow-x: hidden !important;
                        }
                        body {
                            display: flex !important;
                            flex-direction: column !important;
                            align-items: center !important;
                            justify-content: flex-start !important;
                            padding: 16px !important;
                        }
                        .dashboard-container, .main-container, .container {
                            width: 100% !important;
                            max-width: 400px !important;
                            padding: 12px !important;
                            margin: 0 auto !important;
                        }
                        .city-status-card {
                            border-radius: 16px !important;
                            box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
                            margin-bottom: 16px !important;
                            width: 100% !important;
                        }
                        .feature-grid {
                            display: grid !important;
                            grid-template-columns: repeat(2, 1fr) !important;
                            gap: 12px !important;
                            margin: 16px 0 !important;
                            width: 100% !important;
                        }
                        .feature-button {
                            border-radius: 12px !important;
                            padding: 16px !important;
                            text-align: center !important;
                            transition: transform 0.2s ease !important;
                            border: none !important;
                            min-height: 80px !important;
                            width: 100% !important;
                        }
                        .feature-button:active {
                            transform: scale(0.95) !important;
                        }
                        .card-container {
                            border-radius: 16px !important;
                            overflow: hidden !important;
                            margin: 16px 0 !important;
                            width: 100% !important;
                        }
                        input, button {
                            font-size: 16px !important;
                        }
                        button, .btn, .clickable {
                            min-height: 44px !important;
                            min-width: 44px !important;
                        }
                        /* Center all content */
                        #root, .app, .main-content {
                            width: 100% !important;
                            max-width: 400px !important;
                            margin: 0 auto !important;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    if (window.CrowdShiftAndroid) {
                        window.cardId = '$cardId';
                        window.loginMethod = '$loginMethod';
                    }
                })()
            """.trimIndent(), null)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url.toString()
            resetAutoLogoutTimer()

            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "No app found to handle this action", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            return if (url.contains(Uri.parse(webViewUrl).host ?: "")) {
                false
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this@MainActivity, "Connection error. Pull to refresh.", Toast.LENGTH_LONG).show()
        }
    }

    inner class CrowdShiftWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressBar.progress = newProgress

            if (newProgress == 100) {
                progressBar.visibility = View.GONE
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            callback?.invoke(origin, true, false)
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.grant(request.resources)
        }
    }

    override fun onBackPressed() {
        when {
            isNavVisible -> {
                // Hide navigation if visible
                toggleFloatingNavigation()
            }
            webView.canGoBack() -> {
                webView.goBack()
                resetAutoLogoutTimer()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLogoutTimer()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        resetAutoLogoutTimer()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }
        autoHideHandler?.let { mainHandler.removeCallbacks(it) }
        webView.destroy()
        super.onDestroy()
    }
}