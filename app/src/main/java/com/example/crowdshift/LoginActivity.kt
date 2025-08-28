package com.example.crowdshift

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var beepIdInput: EditText
    private lateinit var qrButton: Button
    private lateinit var breatheInButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREF_NAME = "CrowdShiftPrefs"
        private const val KEY_BEEP_ID = "beep_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        // Static valid IDs for now
        private const val MANUAL_PASSWORD = "1234"
        private const val QR_PASSWORD = "1234"
    }

    // Modern permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            showToast("Camera permission denied. QR scanning won't work.")
        }
    }

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedId = result.contents.trim()
            handleScannedBeepId(scannedId)
        } else {
            showToast("QR scan cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
        requestPermissions()

        // Handle logout flag - clear any stored login data
        if (intent.getBooleanExtra("CLEAR_SAVED_LOGIN", false)) {
            clearStoredLoginData()
            showToast("Session expired. Please login again.")
        } else {
            // Auto-login check only if we're not being forced to logout
            checkAutoLogin()
        }
    }

    private fun initializeViews() {
        beepIdInput = findViewById(R.id.beepIdInput)
        qrButton = findViewById(R.id.qrButton)
        breatheInButton = findViewById(R.id.breatheInButton)
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun setupClickListeners() {
        qrButton.setOnClickListener {
            if (checkCameraPermission()) {
                startQRScanner()
            } else {
                requestCameraPermission()
            }
        }

        breatheInButton.setOnClickListener {
            val beepId = beepIdInput.text.toString().trim()
            handleManualLogin(beepId)
        }
    }

    private fun clearStoredLoginData() {
        sharedPreferences.edit().apply {
            remove(KEY_BEEP_ID)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }

        // Clear the input field
        beepIdInput.setText("")
    }

    // Update the checkAutoLogin method to be more robust
    private fun checkAutoLogin() {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val savedBeepId = sharedPreferences.getString(KEY_BEEP_ID, null)

        // Only auto-login if explicitly logged in AND we have valid credentials
        if (isLoggedIn && !savedBeepId.isNullOrEmpty() && savedBeepId.length >= 4) {
            showToast("Welcome back!")
            proceedToMainActivity(savedBeepId)
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun startQRScanner() {
        val options = ScanOptions().apply {
            setPrompt("Scan your Beep Card QR Code")
            setBeepEnabled(true)
            setOrientationLocked(false)
            setBarcodeImageEnabled(true)
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        }
        qrScannerLauncher.launch(options)
    }

    private fun handleManualLogin(beepId: String) {
        when {
            TextUtils.isEmpty(beepId) -> {
                beepIdInput.error = "Please enter your Beep Card ID"
                showToast("Please enter your Beep Card ID")
            }
            beepId == MANUAL_PASSWORD -> {
                processSuccessfulLogin(beepId)
            }
            else -> {
                beepIdInput.error = "Invalid Beep Card ID"
                showToast("Invalid Beep Card ID. Please try again.")
            }
        }
    }

    private fun handleScannedBeepId(scannedId: String) {
        if (scannedId == QR_PASSWORD) {
            beepIdInput.setText(scannedId)
            processSuccessfulLogin(scannedId)
        } else {
            showToast("Invalid QR Code. Try again.")
        }
    }

    private fun processSuccessfulLogin(beepId: String) {
        sharedPreferences.edit().apply {
            putString(KEY_BEEP_ID, beepId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        showToast("Login successful!")
        proceedToMainActivity(beepId)
    }

    // Updated proceedToMainActivity method to pass the correct data
    private fun proceedToMainActivity(beepId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("CARD_ID", beepId)  // Changed from "beepId" to match MainActivity
            putExtra("LOGIN_METHOD", "manual")  // Add login method
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        beepIdInput.error = null
    }
}