package com.example.crowdshift

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
        private const val QR_SCAN_REQUEST_CODE = 1001
        private const val CROWDSHIFT_URL = "https://mrkydev.github.io/CrowdShift-DICT/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupWebView()
        setupSwipeRefresh()
        requestPermissions()

        // Load the CrowdShift login page from assets
        webView.loadUrl(CROWDSHIFT_URL)
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings

        // Enable JavaScript
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true

        // Enable DOM storage
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        // Enable geolocation
        webSettings.setGeolocationEnabled(true)

        // Allow mixed content (HTTP and HTTPS)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Enable zoom controls
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // Cache settings (without deprecated methods)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        // User agent
        webSettings.userAgentString = webSettings.userAgentString + " CrowdShiftApp/1.0"

        // Set WebView client
        webView.webViewClient = CrowdShiftWebViewClient()

        // Set WebChrome client for advanced features
        webView.webChromeClient = CrowdShiftWebChromeClient()

        // Add JavaScript interface for QR scanning
        webView.addJavascriptInterface(WebAppInterface(), "Android")
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Set color scheme using Android system colors to avoid resource issues
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, android.R.color.holo_green_dark),
            ContextCompat.getColor(this, android.R.color.holo_blue_dark),
            ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        )
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Check camera permission (for QR scanning)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissions granted for QR scanning and location",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Some features may not work without permissions",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == QR_SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val qrResult = data?.getStringExtra(QRScannerActivity.EXTRA_QR_RESULT)
                qrResult?.let { result ->
                    // Send QR result to web app
                    val javascript = "javascript:handleQRResult('$result')"
                    webView.evaluateJavascript(javascript, null)

                    Toast.makeText(this, "QR Code: $result", Toast.LENGTH_SHORT).show()
                }
            } else {
                // QR scan cancelled or failed
                val javascript = "javascript:handleQRCancel()"
                webView.evaluateJavascript(javascript, null)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * JavaScript interface for web app communication
     */
    inner class WebAppInterface {
        @JavascriptInterface
        fun openQRScanner() {
            runOnUiThread {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    QRScannerActivity.startForResult(this@MainActivity)
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class CrowdShiftWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            // Allow navigation within local assets and CrowdShift domains
            request?.url?.let { uri ->
                val urlString = uri.toString()
                if (urlString.startsWith("file:///android_asset/") ||
                    uri.host?.contains("crowdshift") == true) {
                    return false // Let WebView handle it
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false

            // Inject mobile optimizations
            injectMobileOptimizations()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            swipeRefreshLayout.isRefreshing = false

            Toast.makeText(
                this@MainActivity,
                "Error loading page. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }

        private fun injectMobileOptimizations() {
            val javascript = """
                javascript:(function() {
                    if (!document.querySelector('meta[name="viewport"]')) {
                        var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, user-scalable=yes';
                        document.getElementsByTagName('head')[0].appendChild(meta);
                    }
                    
                    var style = document.createElement('style');
                    style.textContent = 'body { -webkit-text-size-adjust: 100%; }';
                    document.head.appendChild(style);
                })()
            """.trimIndent()

            webView.evaluateJavascript(javascript, null)
        }
    }

    inner class CrowdShiftWebChromeClient : WebChromeClient() {

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            // Grant geolocation permission for local assets
            if (origin?.startsWith("file://") == true) {
                callback?.invoke(origin, true, false)
            } else {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.let { permissionRequest ->
                if (permissionRequest.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                        permissionRequest.grant(permissionRequest.resources)
                    } else {
                        permissionRequest.deny()
                        Toast.makeText(this@MainActivity,
                            "Camera permission needed for QR scanning",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            supportActionBar?.title = title ?: "CrowdShift"
        }
    }
}