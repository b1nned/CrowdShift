package com.example.crowdshift

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
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

class MainActivity : AppCompatActivity() {

    // UI Components
    lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var floatingNav: LinearLayout
    private lateinit var toggleNavButton: FloatingActionButton
    private lateinit var outsideOverlay: View

    // Configuration
    private val webViewUrl = BuildConfig.WEB_URL

    // User data
    var cardId: String? = null
    var loginMethod: String? = null

    // Navigation state
    private var isNavVisible = false

    // Handlers for async operations
    private val mainHandler = Handler(Looper.getMainLooper())

    // Auto logout configuration
    private val autoLogoutHandler = Handler(Looper.getMainLooper())
    private val autoLogoutDelay = 30 * 60 * 1000L // 30 minutes
    private var autoLogoutRunnable: Runnable? = null

    // Permission handling
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    // QR Code scanning
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { qrContent ->
            handleQRCodeResult(qrContent)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "CrowdShiftPrefs"
        private const val KEY_BEEP_ID = "beep_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Starting MainActivity initialization")

            setContentView(R.layout.activity_main)

            retrieveLoginData()
            initializeViews()
            setupToolbar()
            setupFloatingNavigation()
            setupWebView()
            checkRequiredPermissions()
            handleAppShortcuts()
            loadWebApplication()
            startAutoLogoutTimer()

            Log.d(TAG, "MainActivity initialization completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error during initialization", e)
            handleInitializationError(e)
        }
    }

    /**
     * Retrieve login data from intent
     */
    private fun retrieveLoginData() {
        cardId = intent.getStringExtra("CARD_ID") ?: intent.getStringExtra("beepId")
        loginMethod = intent.getStringExtra("LOGIN_METHOD") ?: "manual"
        Log.d(TAG, "Retrieved login data - Card ID: $cardId, Method: $loginMethod")
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        try {
            webView = findViewById(R.id.webView)
            progressBar = findViewById(R.id.progressBar)
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
            toolbar = findViewById(R.id.toolbar)
            floatingNav = findViewById(R.id.floating_nav)
            toggleNavButton = findViewById(R.id.toggle_nav_button)
            outsideOverlay = findViewById(R.id.outside_overlay)

            setupSwipeRefresh()
            setupNavigationToggle()
            setupOutsideOverlay()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun setupSwipeRefresh() {
        // Configure refresh colors
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.secondary_blue),
            ContextCompat.getColor(this, R.color.accent_orange)
        )

        // Set refresh action
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Pull-to-refresh triggered")
            webView.reload()
            resetAutoLogoutTimer()
        }

        // CRITICAL FIX: Only allow refresh when at the absolute top
        swipeRefreshLayout.setOnChildScrollUpCallback { parent, child ->
            // Return true if child can scroll up (prevents refresh)
            // Return false only when at the very top (allows refresh)
            if (child is WebView) {
                child.scrollY > 0
            } else {
                false
            }
        }

        // Additional control: Monitor WebView scroll position
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            // Enable refresh only when at the absolute top
            val isAtTop = scrollY == 0
            swipeRefreshLayout.isEnabled = isAtTop

            // Debug logging (remove in production)
            if (isAtTop && !swipeRefreshLayout.isEnabled) {
                Log.d(TAG, "SwipeRefresh enabled - at top position")
            } else if (!isAtTop && swipeRefreshLayout.isEnabled) {
                Log.d(TAG, "SwipeRefresh disabled - scrolled down")
            }

            // Reset auto-logout timer on any scroll
            resetAutoLogoutTimer()
        }

        // Set initial state
        swipeRefreshLayout.isEnabled = true
    }

    // Alternative method if you want even more control:
    private fun setupAdvancedSwipeRefresh() {
        var isRefreshAllowed = true
        var lastScrollY = 0

        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.secondary_blue),
            ContextCompat.getColor(this, R.color.accent_orange)
        )

        swipeRefreshLayout.setOnRefreshListener {
            if (isRefreshAllowed) {
                Log.d(TAG, "Refresh allowed - reloading page")
                webView.reload()
                resetAutoLogoutTimer()
            } else {
                Log.d(TAG, "Refresh blocked - not at top")
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Precise scroll tracking
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            lastScrollY = scrollY

            // Only allow refresh when exactly at the top
            isRefreshAllowed = scrollY == 0
            swipeRefreshLayout.isEnabled = isRefreshAllowed

            // Prevent accidental refresh when scrolling back to top
            if (scrollY == 0 && oldScrollY > 0) {
                // Just reached the top - add small delay before enabling refresh
                Handler(Looper.getMainLooper()).postDelayed({
                    if (webView.scrollY == 0) {
                        swipeRefreshLayout.isEnabled = true
                        Log.d(TAG, "Refresh enabled after reaching top")
                    }
                }, 200) // 200ms delay
            }

            resetAutoLogoutTimer()
        }

        // Handle edge cases with touch events
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            val canScrollUp = webView.scrollY > 0
            Log.d(TAG, "CanScrollUp check: $canScrollUp (scrollY: ${webView.scrollY})")
            canScrollUp
        }
    }

    // Enhanced version with touch event handling (most robust solution)
    private fun setupRobustSwipeRefresh() {
        var isAtTop = true
        var touchStartY = 0f
        var isRefreshGesture = false

        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.secondary_blue),
            ContextCompat.getColor(this, R.color.accent_orange)
        )

        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Refresh triggered - reloading page")
            webView.reload()
            resetAutoLogoutTimer()
        }

        // Monitor WebView scroll position
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            isAtTop = scrollY == 0

            // Enable/disable swipe refresh based on position
            swipeRefreshLayout.isEnabled = isAtTop

            Log.d(TAG, "Scroll: Y=$scrollY, isAtTop=$isAtTop, refreshEnabled=${swipeRefreshLayout.isEnabled}")
            resetAutoLogoutTimer()
        }

        // Critical callback - this is what actually controls refresh availability
        swipeRefreshLayout.setOnChildScrollUpCallback { parent, child ->
            when (child) {
                is WebView -> {
                    val canScrollUp = child.scrollY > 0
                    Log.d(TAG, "Child scroll up check: canScrollUp=$canScrollUp, scrollY=${child.scrollY}")
                    canScrollUp
                }
                else -> false
            }
        }

        // Optional: Handle touch events for even more precise control
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.y
                    isRefreshGesture = false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - touchStartY
                    // Only consider it a refresh gesture if starting from top and moving down
                    isRefreshGesture = isAtTop && deltaY > 50 && webView.scrollY == 0
                }
            }
            false // Don't consume the event
        }
    }

    // Simple and effective solution (recommended)
    private fun setupSimpleSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.secondary_blue),
            ContextCompat.getColor(this, R.color.accent_orange)
        )

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            resetAutoLogoutTimer()
        }

        // The key fix: This callback determines when refresh is allowed
        swipeRefreshLayout.setOnChildScrollUpCallback { _, child ->
            // If child is WebView and can scroll up, prevent refresh
            if (child is WebView) {
                child.scrollY > 0  // true = can scroll up (no refresh), false = at top (allow refresh)
            } else {
                false
            }
        }
    }

    private fun setupNavigationToggle() {
        floatingNav.visibility = View.GONE
        isNavVisible = false

        toggleNavButton.setOnClickListener {
            toggleFloatingNavigation()
        }
    }

    private fun setupOutsideOverlay() {
        outsideOverlay.setOnClickListener {
            hideNavigationWithAnimation()
        }
    }

    /**
     * Setup action bar/toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.app_name).uppercase()
            show()
        }
    }

    /**
     * Toggle floating navigation visibility with proper animation
     */
    private fun toggleFloatingNavigation() {
        if (isNavVisible) {
            hideNavigationWithAnimation()
        } else {
            showNavigationWithAnimation()
        }

        // Animate FAB rotation
        animateFABRotation()
    }

    private fun animateFABRotation() {
        val targetRotation = if (isNavVisible) 180f else 0f
        ObjectAnimator.ofFloat(toggleNavButton, "rotation", targetRotation).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * Show navigation with smooth animation
     */
    private fun showNavigationWithAnimation() {
        floatingNav.visibility = View.VISIBLE
        outsideOverlay.visibility = View.VISIBLE
        outsideOverlay.isClickable = true
        outsideOverlay.isFocusable = true

        // Animate overlay fade in
        outsideOverlay.alpha = 0f
        outsideOverlay.animate()
            .alpha(0.5f)
            .setDuration(300)
            .start()

        // Animate navigation slide in
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

    /**
     * Hide navigation with smooth animation
     */
    private fun hideNavigationWithAnimation() {
        if (!isNavVisible) return

        // Animate overlay fade out
        outsideOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                outsideOverlay.visibility = View.GONE
                outsideOverlay.isClickable = false
                outsideOverlay.isFocusable = false
            }
            .start()

        // Animate navigation slide out
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

    /**
     * Enhanced floating navigation with better targeting and fallbacks
     */
    private fun setupFloatingNavigation() {
        // Dashboard - intelligent refresh and navigation
        findViewById<ImageButton>(R.id.nav_dashboard).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        // Multiple strategies to return to dashboard
                        
                        // Strategy 1: Look for dashboard elements
                        const dashboardElements = document.querySelectorAll(`
                            [id*="dashboard"], [class*="dashboard"], [data-page="dashboard"],
                            .main-content, .home, [role="main"], main
                        `);
                        
                        if (dashboardElements.length > 0) {
                            dashboardElements[0].scrollIntoView({ behavior: 'smooth', block: 'start' });
                        } else {
                            // Strategy 2: Scroll to top
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                        }
                        
                        // Strategy 3: Refresh data if functions exist
                        if (typeof refreshDashboard === 'function') refreshDashboard();
                        if (typeof updateCityStatus === 'function') updateCityStatus();
                        if (typeof loadRecommendations === 'function') loadRecommendations();
                        
                        // Strategy 4: Trigger refresh on elements
                        document.querySelectorAll('[data-refresh], [id$="-refresh"], .refresh-btn').forEach(btn => {
                            if (btn.click) btn.click();
                        });
                        
                        return 'dashboard_accessed';
                    } catch(e) {
                        console.error('Dashboard navigation error:', e);
                        window.scrollTo({ top: 0, behavior: 'smooth' });
                        return 'dashboard_fallback';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // City Status - enhanced targeting
        findViewById<ImageButton>(R.id.nav_city_status).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        // Enhanced selector strategy
                        const cityStatusSelectors = [
                            '#city-status', '.city-status', '[data-section="city-status"]',
                            '.city-status-card', '[class*="city-status"]', '[id*="city-status"]',
                            'section:has-text("City Status")', '[data-testid*="city"]'
                        ];
                        
                        for (let selector of cityStatusSelectors) {
                            try {
                                const element = document.querySelector(selector);
                                if (element && element.offsetParent !== null) { // Check if visible
                                    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                    
                                    // Highlight the section briefly
                                    element.style.transition = 'background-color 0.5s';
                                    element.style.backgroundColor = '#E8F5E9';
                                    setTimeout(() => {
                                        element.style.backgroundColor = '';
                                    }, 1000);
                                    
                                    return 'city_status_found_' + selector;
                                }
                            } catch(e) {
                                console.log('Selector failed:', selector, e);
                            }
                        }
                        
                        // Fallback: look for text content
                        const textElements = Array.from(document.querySelectorAll('*')).filter(el => {
                            return el.textContent.toLowerCase().includes('city status') && 
                                   el.offsetParent !== null &&
                                   el.tagName !== 'SCRIPT' && el.tagName !== 'STYLE';
                        });
                        
                        if (textElements.length > 0) {
                            textElements[0].scrollIntoView({ behavior: 'smooth', block: 'start' });
                            return 'city_status_text_found';
                        }
                        
                        // Ultimate fallback
                        window.scrollTo({ top: document.body.scrollHeight * 0.2, behavior: 'smooth' });
                        return 'city_status_scroll_fallback';
                        
                    } catch(e) {
                        console.error('City status navigation error:', e);
                        return 'city_status_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // QR Scanner - launch native scanner
        findViewById<ImageButton>(R.id.nav_scan_qr).setOnClickListener {
            startQRScanner()
            hideNavigationWithAnimation()
        }

        // Top Up - enhanced detection and triggering
        findViewById<ImageButton>(R.id.nav_top_up).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        // Enhanced top-up detection
                        const topupSelectors = [
                            '[data-action="topup"]', '.js-topup-open', '[class*="topup"]',
                            'button:contains("Top Up")', 'button:contains("Top-up")', 
                            'button:contains("Add Balance")', '[id*="topup"]',
                            '.feature-button[data-feature="topup"]',
                            'a[href*="topup"]'
                        ];
                        
                        for (let selector of topupSelectors) {
                            try {
                                const element = document.querySelector(selector);
                                if (element && element.offsetParent !== null) {
                                    if (element.click) {
                                        element.click();
                                        return 'topup_clicked_' + selector;
                                    }
                                }
                            } catch(e) {
                                console.log('Topup selector failed:', selector, e);
                            }
                        }
                        
                        // Enhanced text-based search
                        const textButtons = Array.from(document.querySelectorAll('button, a, [role="button"]')).filter(btn => {
                            const text = btn.textContent.toLowerCase().trim();
                            return (text.includes('top') && text.includes('up')) ||
                                   text.includes('add balance') ||
                                   text.includes('reload') ||
                                   text.includes('charge');
                        });
                        
                        if (textButtons.length > 0) {
                            textButtons[0].click();
                            return 'topup_text_clicked';
                        }
                        
                        // Look for quick actions section
                        const quickActionsSelectors = [
                            '#quick-actions', '.quick-actions', '[class*="quick-action"]',
                            '.feature-grid', '[data-section="actions"]'
                        ];
                        
                        for (let selector of quickActionsSelectors) {
                            const element = document.querySelector(selector);
                            if (element) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                return 'quick_actions_scrolled';
                            }
                        }
                        
                        // Show native top-up if available
                        if (window.CrowdShiftAndroid) {
                            window.CrowdShiftAndroid.showToast('Top-up feature coming soon! Use quick actions below.');
                            window.scrollTo({ top: document.body.scrollHeight * 0.7, behavior: 'smooth' });
                            return 'native_topup_message';
                        }
                        
                        return 'topup_not_found';
                        
                    } catch(e) {
                        console.error('Top-up navigation error:', e);
                        return 'topup_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // Shuttle Routes - enhanced navigation
        findViewById<ImageButton>(R.id.nav_shuttle_routes).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        const shuttleSelectors = [
                            '#shuttle-routes', '.shuttle-routes', '[data-section="shuttle"]',
                            '[class*="shuttle"]', '[id*="shuttle"]', '[data-feature="shuttle"]'
                        ];
                        
                        for (let selector of shuttleSelectors) {
                            const element = document.querySelector(selector);
                            if (element && element.offsetParent !== null) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                return 'shuttle_section_found';
                            }
                        }
                        
                        // Look for text-based shuttle elements
                        const textElements = Array.from(document.querySelectorAll('*')).filter(el => {
                            const text = el.textContent.toLowerCase();
                            return (text.includes('shuttle') || text.includes('bus') || text.includes('transport')) &&
                                   el.offsetParent !== null &&
                                   text.length < 200; // Avoid large paragraphs
                        });
                        
                        if (textElements.length > 0) {
                            textElements[0].scrollIntoView({ behavior: 'smooth', block: 'start' });
                            return 'shuttle_text_found';
                        }
                        
                        // Show comprehensive shuttle information
                        if (window.CrowdShiftAndroid) {
                            window.CrowdShiftAndroid.showNativeDialog(
                                'Shuttle Routes',
                                'Available Routes:\\n\\n🚌 Route 1: City Center ↔ Tourist Spots\\n   Every 15 minutes\\n\\n🚌 Route 2: Hotels ↔ Shopping Areas\\n   Every 20 minutes\\n\\n🚌 Route 3: Airport ↔ Session Road\\n   Every 30 minutes\\n\\nTap any shuttle booking button in the app to reserve your seat!'
                            );
                        }
                        
                        return 'shuttle_info_displayed';
                        
                    } catch(e) {
                        console.error('Shuttle navigation error:', e);
                        return 'shuttle_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // Find Parking - enhanced parking navigation
        findViewById<ImageButton>(R.id.nav_find_parking).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        const parkingSelectors = [
                            '#parking-availability', '#parking', '.parking-availability',
                            '[data-section="parking"]', '[class*="parking"]', '[id*="parking"]'
                        ];
                        
                        for (let selector of parkingSelectors) {
                            const element = document.querySelector(selector);
                            if (element && element.offsetParent !== null) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                
                                // Highlight parking section
                                element.style.transition = 'box-shadow 0.5s';
                                element.style.boxShadow = '0 0 20px rgba(76, 175, 80, 0.5)';
                                setTimeout(() => {
                                    element.style.boxShadow = '';
                                }, 2000);
                                
                                return 'parking_section_found';
                            }
                        }
                        
                        // Show parking information with current status
                        if (window.CrowdShiftAndroid) {
                            window.CrowdShiftAndroid.showNativeDialog(
                                'Parking Availability',
                                '🅿️ Real-time Parking Status:\\n\\n📍 Burnham Park\\n   Available: 85/150 slots\\n   Rate: ₱25/hour\\n\\n📍 Session Road\\n   Available: 12/80 slots\\n   Rate: ₱40/hour\\n\\n📍 SM Baguio\\n   Available: 245/300 slots\\n   Rate: ₱30/hour\\n\\nScroll down to find parking reservation buttons!'
                            );
                        }
                        
                        // Scroll to likely parking section
                        window.scrollTo({ top: document.body.scrollHeight * 0.6, behavior: 'smooth' });
                        
                        return 'parking_info_displayed';
                        
                    } catch(e) {
                        console.error('Parking navigation error:', e);
                        return 'parking_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // Tourist Spots - enhanced recommendations navigation
        findViewById<ImageButton>(R.id.nav_tourist_spots).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        const recoSelectors = [
                            '#smart-recommendations', '.smart-recommendations', 
                            '#recommendations', '.recommendations',
                            '[data-section="recommendations"]', '[class*="recommendation"]',
                            '[class*="tourist"]', '[id*="tourist"]', '.curated-spots'
                        ];
                        
                        for (let selector of recoSelectors) {
                            const element = document.querySelector(selector);
                            if (element && element.offsetParent !== null) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                
                                // Animate recommendation cards if they exist
                                const cards = element.querySelectorAll('[class*="card"], [class*="item"]');
                                cards.forEach((card, index) => {
                                    setTimeout(() => {
                                        card.style.transform = 'scale(1.02)';
                                        card.style.transition = 'transform 0.3s';
                                        setTimeout(() => {
                                            card.style.transform = 'scale(1)';
                                        }, 300);
                                    }, index * 100);
                                });
                                
                                return 'recommendations_found';
                            }
                        }
                        
                        // Look for tourist-related text
                        const textElements = Array.from(document.querySelectorAll('*')).filter(el => {
                            const text = el.textContent.toLowerCase();
                            return (text.includes('tourist') || text.includes('recommendation') || 
                                   text.includes('spot') || text.includes('attraction')) &&
                                   el.offsetParent !== null && 
                                   text.length < 300;
                        });
                        
                        if (textElements.length > 0) {
                            textElements[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
                            return 'tourist_text_found';
                        }
                        
                        // Scroll to middle area where recommendations typically are
                        const middleY = document.body.scrollHeight * 0.45;
                        window.scrollTo({ top: middleY, behavior: 'smooth' });
                        
                        return 'recommendations_middle_scroll';
                        
                    } catch(e) {
                        console.error('Recommendations navigation error:', e);
                        return 'recommendations_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // Settings - enhanced settings access
        findViewById<ImageButton>(R.id.nav_settings).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        const settingsSelectors = [
                            '[data-action="settings"]', '.js-qa[data-action="settings"]',
                            '#settings-modal', '.settings-modal', '[id*="settings"]',
                            'button:contains("Settings")', '[class*="settings"]'
                        ];
                        
                        for (let selector of settingsSelectors) {
                            const element = document.querySelector(selector);
                            if (element) {
                                if (element.id && element.id.includes('modal')) {
                                    element.style.display = 'flex';
                                    return 'settings_modal_opened';
                                } else if (element.click) {
                                    element.click();
                                    return 'settings_clicked';
                                }
                            }
                        }
                        
                        // Show native settings dialog
                        if (window.CrowdShiftAndroid) {
                            const confirmed = confirm('⚙️ Settings\\n\\nOpen device settings to manage app permissions and preferences?');
                            if (confirmed) {
                                window.CrowdShiftAndroid.openSettings();
                            }
                            return 'native_settings_prompted';
                        }
                        
                        return 'settings_not_found';
                        
                    } catch(e) {
                        console.error('Settings navigation error:', e);
                        return 'settings_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // Help - comprehensive help system
        findViewById<ImageButton>(R.id.nav_help).setOnClickListener {
            executeWebViewScript("""
                (function() {
                    try {
                        const helpSelectors = [
                            '[data-action="help"]', '.js-qa[data-action="help"]',
                            '#help-modal', '.help-modal', '[id*="help"]'
                        ];
                        
                        for (let selector of helpSelectors) {
                            const element = document.querySelector(selector);
                            if (element) {
                                if (element.id && element.id.includes('modal')) {
                                    element.style.display = 'flex';
                                    return 'help_modal_opened';
                                } else if (element.click) {
                                    element.click();
                                    return 'help_clicked';
                                }
                            }
                        }
                        
                        // Show comprehensive help
                        if (window.CrowdShiftAndroid) {
                            window.CrowdShiftAndroid.showNativeDialog(
                                'CrowdShift Help',
                                '📱 How to use CrowdShift:\\n\\n' +
                                '🎫 Smart Cards: Tap buttons to purchase and manage cards\\n\\n' +
                                '📊 City Status: View real-time crowd data and congestion levels\\n\\n' +
                                '🚌 Shuttles: Book seats in advance for better rates and guaranteed spots\\n\\n' +
                                '📱 QR Codes: Scan QR codes for quick actions and payments\\n\\n' +
                                '🅿️ Parking: Check live availability and reserve spots ahead of time\\n\\n' +
                                '📍 Navigation: Get optimized routes that avoid crowded areas\\n\\n' +
                                '💡 Tips: Pull down to refresh data, use the floating menu for quick access!'
                            );
                        }
                        
                        return 'help_info_displayed';
                        
                    } catch(e) {
                        console.error('Help navigation error:', e);
                        return 'help_error';
                    }
                })()
            """)
            hideNavigationWithAnimation()
        }

        // About - show app information
        findViewById<ImageButton>(R.id.nav_about).setOnClickListener {
            showAboutDialog()
            hideNavigationWithAnimation()
        }

        // Logout - confirm and perform logout
        findViewById<ImageButton>(R.id.nav_logout).setOnClickListener {
            showLogoutConfirmDialog()
            hideNavigationWithAnimation()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About CrowdShift")
            .setIcon(R.drawable.ic_info)
            .setMessage("""
                CrowdShift v${getAppVersionName()}
                
                Smart tourism management for Baguio City.
                
                Developed to reduce congestion and improve visitor experience through real-time data and intelligent routing.
                
                "Less Congestions, More Connections"
                
                Features:
                • Real-time crowd monitoring
                • Smart route optimization
                • Integrated payment system
                • Tourist spot recommendations
                • Parking availability tracking
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getAppVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Execute JavaScript in WebView with enhanced error handling and logging
     */
    private fun executeWebViewScript(script: String, callback: ValueCallback<String>? = null) {
        try {
            val wrappedScript = """
                javascript:(function() {
                    try {
                        console.log('Executing navigation script...');
                        $script
                    } catch(e) {
                        console.error('Script execution error:', e);
                        return 'script_error: ' + e.message;
                    }
                })()
            """.trimIndent()

            webView.evaluateJavascript(wrappedScript) { result ->
                Log.d(TAG, "Script result: $result")
                callback?.onReceiveValue(result)

                // Provide user feedback for failed operations
                if (result?.contains("not_found") == true || result?.contains("error") == true) {
                    mainHandler.post {
                        Toast.makeText(this, "Feature accessed via menu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing WebView script", e)
        }
    }

    /**
     * Setup WebView with optimal configuration
     */
    private fun setupWebView() {
        webView.apply {
            webViewClient = CrowdShiftWebViewClient()
            webChromeClient = CrowdShiftWebChromeClient()

            settings.apply {
                // Enable JavaScript and storage
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                // File access
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                // Zoom and viewport
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true

                // Cache and loading
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // Media and location
                setGeolocationEnabled(true)
                mediaPlaybackRequiresUserGesture = false

                // Performance and rendering
                setRenderPriority(WebSettings.RenderPriority.HIGH)
                textZoom = 100
                defaultFontSize = 16
                defaultFixedFontSize = 13

                // User agent
                userAgentString = "$userAgentString CrowdShift/${getAppVersionName()} Mobile"
            }

            // Add JavaScript interface
            addJavascriptInterface(CrowdShiftJSInterface(this@MainActivity), "CrowdShiftAndroid")
        }
    }

    /**
     * Check and request required permissions
     */
    private fun checkRequiredPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (deniedPermissions.isNotEmpty()) {
            val message = when {
                deniedPermissions.contains(Manifest.permission.CAMERA) ->
                    "Camera permission denied. QR code scanning won't work."
                deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ->
                    "Location permission denied. Some features may be limited."
                else -> "Some permissions were denied. App functionality may be limited."
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle app shortcuts and deep links
     */
    private fun handleAppShortcuts() {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    when (uri.scheme) {
                        "crowdshift" -> {
                            when (uri.host) {
                                "qr-scan" -> startQRScanner()
                                "city-status" -> scrollToCityStatus()
                                else -> Log.d(TAG, "Unknown deep link: $uri")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scrollToCityStatus() {
        mainHandler.postDelayed({
            executeWebViewScript("""
                (function() {
                    const cityStatus = document.querySelector('#city-status, [class*="city-status"]');
                    if (cityStatus) {
                        cityStatus.scrollIntoView({ behavior: 'smooth' });
                    }
                })()
            """)
        }, 1000) // Delay to ensure page is loaded
    }

    /**
     * Load the web application
     */
    private fun loadWebApplication() {
        val url = buildString {
            append(webViewUrl)
            if (cardId != null) {
                append("?cardId=$cardId&method=$loginMethod")
            }
        }

        Log.d(TAG, "Loading web app: $url")
        webView.loadUrl(url)
    }

    /**
     * Launch QR code scanner
     */
    fun startQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show()
            return
        }

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

    /**
     * Handle QR code scan results
     */
    private fun handleQRCodeResult(qrContent: String) {
        resetAutoLogoutTimer()

        Log.d(TAG, "QR Code scanned: $qrContent")

        when {
            qrContent.startsWith("crowdshift://") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrContent))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling crowdshift URL", e)
                    Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                }
            }
            qrContent.contains("topup", ignoreCase = true) -> {
                executeWebViewScript("if (typeof handleTopUpQR === 'function') { handleTopUpQR('$qrContent'); }")
            }
            qrContent.contains("parking", ignoreCase = true) -> {
                executeWebViewScript("if (typeof handleParkingQR === 'function') { handleParkingQR('$qrContent'); }")
            }
            else -> {
                executeWebViewScript("if (typeof handleQRCode === 'function') { handleQRCode('$qrContent'); }")
            }
        }

        Toast.makeText(this, "QR Code processed successfully", Toast.LENGTH_SHORT).show()
    }

    /**
     * Auto logout functionality
     */
    private fun startAutoLogoutTimer() {
        autoLogoutRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                showAutoLogoutDialog()
            }
        }
        autoLogoutHandler.postDelayed(autoLogoutRunnable!!, autoLogoutDelay)
    }

    private fun resetAutoLogoutTimer() {
        autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }
        startAutoLogoutTimer()
    }

    private fun showAutoLogoutDialog() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Session Timeout")
            .setMessage("You've been inactive for 30 minutes. Do you want to continue your session?")
            .setIcon(R.drawable.ic_help)
            .setPositiveButton("Continue") { _, _ ->
                resetAutoLogoutTimer()
            }
            .setNegativeButton("Logout") { _, _ ->
                performLogout()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Perform logout with proper cleanup
     */
    fun performLogout() {
        Log.d(TAG, "Starting logout process")

        // Stop timers
        autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }

        // Clear web application state
        executeWebViewScript("""
            (function() {
                // Try various logout functions
                const logoutFunctions = ['logout', 'signOut', 'clearSession', 'endSession'];
                for (let func of logoutFunctions) {
                    if (typeof window[func] === 'function') {
                        try {
                            window[func]();
                            break;
                        } catch(e) {
                            console.log('Failed to call ' + func + ':', e);
                        }
                    }
                }
                
                // Clear storage
                try {
                    localStorage.clear();
                    sessionStorage.clear();
                } catch(e) {
                    console.log('Could not clear storage:', e);
                }
                
                // Clear cookies
                document.cookie.split(";").forEach(function(c) { 
                    document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"); 
                });
                
                return 'logout_completed';
            })()
        """) { result ->
            Log.d(TAG, "Web logout result: $result")
            mainHandler.post { completeLogout() }
        }
    }

    private fun completeLogout() {
        try {
            // Clear WebView data
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()

            // Clear cookies
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies { success ->
                    Log.d(TAG, "Cookies cleared: $success")
                }
                CookieManager.getInstance().flush()
            } else {
                @Suppress("DEPRECATION")
                CookieManager.getInstance().removeAllCookie()
            }

            // Clear WebStorage
            WebStorage.getInstance().deleteAllData()

            // Clear shared preferences
            clearLoginPreferences()

            // Reset user data
            cardId = null
            loginMethod = null

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate to login with delay
            mainHandler.postDelayed({
                navigateToLogin()
            }, 500)

        } catch (e: Exception) {
            Log.e(TAG, "Error during logout cleanup", e)
            // Force navigation even if cleanup fails
            navigateToLogin()
        }
    }

    private fun clearLoginPreferences() {
        try {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_BEEP_ID)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preferences", e)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CLEAR_SAVED_LOGIN", true)
        }

        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from CrowdShift?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Yes, Logout") { _, _ ->
                progressBar.visibility = View.VISIBLE
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    /**
     * Handle initialization errors
     */
    private fun handleInitializationError(error: Exception) {
        AlertDialog.Builder(this)
            .setTitle("Initialization Error")
            .setMessage("Failed to initialize the app. Please try restarting.\n\nError: ${error.message}")
            .setPositiveButton("Restart") { _, _ ->
                restartApp()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun restartApp() {
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * WebView Client for handling page navigation
     */
    inner class CrowdShiftWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
            resetAutoLogoutTimer()
            Log.d(TAG, "Page started loading: $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false

            Log.d(TAG, "Page finished loading: $url")

            // Inject mobile optimization CSS and functionality
            injectMobileOptimizations(view)
        }

        private fun injectMobileOptimizations(view: WebView?) {
            view?.evaluateJavascript("""
                javascript:(function() {
                    // Add mobile viewport if not present
                    if (!document.querySelector('meta[name="viewport"]')) {
                        var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                        document.getElementsByTagName('head')[0].appendChild(meta);
                    }
                    
                    // Inject mobile-optimized styles
                    var style = document.createElement('style');
                    style.innerHTML = `
                        * { 
                            -webkit-tap-highlight-color: rgba(0,0,0,0.1) !important;
                            box-sizing: border-box !important;
                        }
                        
                        html, body { 
                            -webkit-touch-callout: none;
                            -webkit-user-select: none;
                            -khtml-user-select: none;
                            -moz-user-select: none;
                            -ms-user-select: none;
                            user-select: none;
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            padding: 0 !important;
                            margin: 0 !important;
                            width: 100% !important;
                            height: 100vh !important;
                            overflow-x: hidden !important;
                            background-color: #f5f5f5 !important;
                        }
                        
                        body {
                            display: flex !important;
                            flex-direction: column !important;
                            align-items: center !important;
                            justify-content: flex-start !important;
                            padding: 16px 16px 80px 16px !important;
                            min-height: 100vh !important;
                        }
                        
                        .dashboard-container, 
                        .main-container, 
                        .container,
                        .app-container {
                            width: 100% !important;
                            max-width: 400px !important;
                            padding: 12px !important;
                            margin: 0 auto !important;
                            background-color: transparent !important;
                        }
                        
                        .city-status-card,
                        .status-card {
                            border-radius: 16px !important;
                            box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
                            margin-bottom: 16px !important;
                            width: 100% !important;
                            background: white !important;
                            padding: 16px !important;
                        }
                        
                        .feature-grid,
                        .quick-actions {
                            display: grid !important;
                            grid-template-columns: repeat(3, 1fr) !important;
                            gap: 12px !important;
                            margin: 16px 0 !important;
                            width: 100% !important;
                        }
                        
                        .feature-button,
                        .quick-action-btn {
                            border-radius: 12px !important;
                            padding: 16px 8px !important;
                            text-align: center !important;
                            transition: all 0.2s ease !important;
                            border: none !important;
                            min-height: 80px !important;
                            width: 100% !important;
                            background: white !important;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
                            cursor: pointer !important;
                            display: flex !important;
                            flex-direction: column !important;
                            align-items: center !important;
                            justify-content: center !important;
                        }
                        
                        .feature-button:active,
                        .quick-action-btn:active {
                            transform: scale(0.95) !important;
                            box-shadow: 0 1px 4px rgba(0,0,0,0.2) !important;
                        }
                        
                        .card-container,
                        .all-in-one-card {
                            border-radius: 16px !important;
                            overflow: hidden !important;
                            margin: 16px 0 !important;
                            width: 100% !important;
                            box-shadow: 0 4px 16px rgba(0,0,0,0.15) !important;
                        }
                        
                        input, textarea {
                            font-size: 16px !important;
                            border-radius: 8px !important;
                            border: 1px solid #ddd !important;
                            padding: 12px !important;
                        }
                        
                        button, .btn, .clickable {
                            min-height: 44px !important;
                            min-width: 44px !important;
                            font-size: 16px !important;
                            cursor: pointer !important;
                        }
                        
                        /* Specific CrowdShift styling */
                        #root, .app, .main-content {
                            width: 100% !important;
                            max-width: 400px !important;
                            margin: 0 auto !important;
                        }
                        
                        .city-status h2,
                        .city-status-title {
                            color: #2E8B57 !important;
                            font-size: 18px !important;
                            margin-bottom: 12px !important;
                        }
                        
                        .status-indicator {
                            display: flex !important;
                            align-items: center !important;
                            padding: 8px 12px !important;
                            border-radius: 20px !important;
                            font-size: 14px !important;
                            font-weight: 500 !important;
                        }
                        
                        .status-normal {
                            background-color: #E8F5E9 !important;
                            color: #2E7D32 !important;
                        }
                        
                        .status-warning {
                            background-color: #FFF3E0 !important;
                            color: #F57C00 !important;
                        }
                        
                        .status-busy {
                            background-color: #FFEBEE !important;
                            color: #D32F2F !important;
                        }
                        
                        /* Smooth scrolling */
                        html {
                            scroll-behavior: smooth !important;
                        }
                        
                        /* Loading states */
                        .loading {
                            opacity: 0.6 !important;
                            pointer-events: none !important;
                        }
                        
                        /* Modal improvements */
                        .modal, .modal-overlay {
                            z-index: 9999 !important;
                        }
                        
                        /* Bottom navigation safe area */
                        body::after {
                            content: '';
                            height: 60px;
                            display: block;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    // Set up global variables for web app
                    if (window.CrowdShiftAndroid) {
                        window.cardId = '$cardId';
                        window.loginMethod = '$loginMethod';
                        window.isNativeApp = true;
                        
                        // Add native app identifier
                        document.body.classList.add('native-android-app');
                    }
                    
                    // Disable text selection on touch
                    document.addEventListener('selectstart', function(e) {
                        e.preventDefault();
                    });
                    
                    // Prevent zoom on double tap
                    var lastTouchEnd = 0;
                    document.addEventListener('touchend', function(event) {
                        var now = (new Date()).getTime();
                        if (now - lastTouchEnd <= 300) {
                            event.preventDefault();
                        }
                        lastTouchEnd = now;
                    }, false);
                    
                    return 'mobile_optimizations_applied';
                })()
            """.trimIndent(), null)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            resetAutoLogoutTimer()

            Log.d(TAG, "URL loading requested: $url")

            return when {
                // Handle tel, mailto, sms links
                url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") -> {
                    handleExternalIntent(url)
                    true
                }

                // Handle app-specific URLs
                url.startsWith("crowdshift://") -> {
                    handleCrowdShiftUrl(url)
                    true
                }

                // Allow same-domain navigation
                url.contains(Uri.parse(webViewUrl).host ?: "") -> {
                    false
                }

                // Handle external URLs
                else -> {
                    handleExternalIntent(url)
                    true
                }
            }
        }

        private fun handleExternalIntent(url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening external URL: $url", e)
                val message = when {
                    url.startsWith("tel:") -> "No phone app found"
                    url.startsWith("mailto:") -> "No email app found"
                    url.startsWith("sms:") -> "No messaging app found"
                    else -> "Cannot open link"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        private fun handleCrowdShiftUrl(url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling CrowdShift URL: $url", e)
                Toast.makeText(this@MainActivity, "Invalid app link", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)

            val errorCode = error?.errorCode ?: -1
            val description = error?.description?.toString() ?: "Unknown error"
            val url = request?.url?.toString() ?: "Unknown URL"

            Log.e(TAG, "WebView error - Code: $errorCode, Description: $description, URL: $url")

            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false

            // Only show error for main frame requests
            if (request?.isForMainFrame == true) {
                showConnectionError()
            }
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)

            val statusCode = errorResponse?.statusCode ?: 0
            val url = request?.url?.toString() ?: "Unknown URL"

            Log.w(TAG, "HTTP error - Status: $statusCode, URL: $url")

            // Only handle main frame HTTP errors
            if (request?.isForMainFrame == true && statusCode >= 400) {
                mainHandler.post {
                    showConnectionError()
                }
            }
        }

        private fun showConnectionError() {
            Toast.makeText(
                this@MainActivity,
                "Connection error. Pull down to refresh.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * WebChromeClient for handling advanced web features
     */
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
            // Grant geolocation permission for trusted origins
            val trustedOrigins = listOf(
                webViewUrl,
                "https://larkaholic.github.io",
                "https://crowdshift.app"
            )

            val isOriginTrusted = trustedOrigins.any { origin?.startsWith(it) == true }

            if (isOriginTrusted && areLocationPermissionsGranted()) {
                callback?.invoke(origin, true, false)
            } else {
                callback?.invoke(origin, false, false)
                if (!areLocationPermissionsGranted()) {
                    Toast.makeText(this@MainActivity, "Location permission required for this feature", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun areLocationPermissionsGranted(): Boolean {
            return ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.let { permissionRequest ->
                val supportedResources = mutableListOf<String>()

                permissionRequest.resources.forEach { resource ->
                    when (resource) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                supportedResources.add(resource)
                            }
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                supportedResources.add(resource)
                            }
                        }
                    }
                }

                if (supportedResources.isNotEmpty()) {
                    permissionRequest.grant(supportedResources.toTypedArray())
                } else {
                    permissionRequest.deny()
                }
            }
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let { msg ->
                val logLevel = when (msg.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                    ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                    ConsoleMessage.MessageLevel.DEBUG -> Log.DEBUG
                    else -> Log.INFO
                }

                Log.println(logLevel, "WebView", "Console: ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
            }
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("CrowdShift")
                .setMessage(message ?: "")
                .setPositiveButton("OK") { _, _ ->
                    result?.confirm()
                }
                .setOnDismissListener {
                    result?.confirm()
                }
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("CrowdShift")
                .setMessage(message ?: "")
                .setPositiveButton("OK") { _, _ ->
                    result?.confirm()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    result?.cancel()
                }
                .setOnDismissListener {
                    result?.cancel()
                }
                .show()
            return true
        }
    }

    /**
     * Handle back button navigation
     */
    override fun onBackPressed() {
        when {
            isNavVisible -> {
                hideNavigationWithAnimation()
            }
            webView.canGoBack() -> {
                webView.goBack()
                resetAutoLogoutTimer()
            }
            else -> {
                showExitConfirmDialog()
            }
        }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit CrowdShift")
            .setMessage("Are you sure you want to exit the app?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    /**
     * Activity lifecycle methods
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLogoutTimer()
    }

    override fun onResume() {
        super.onResume()
        try {
            webView.onResume()
            resetAutoLogoutTimer()
            Log.d(TAG, "Activity resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            webView.onPause()
            Log.d(TAG, "Activity paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause", e)
        }
    }

    override fun onDestroy() {
        try {
            // Clean up timers
            autoLogoutRunnable?.let { autoLogoutHandler.removeCallbacks(it) }

            // Clean up WebView
            webView.removeJavascriptInterface("CrowdShiftAndroid")
            webView.destroy()

            Log.d(TAG, "Activity destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }

        super.onDestroy()
    }

}