package com.example.crowdshift.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {

    // Permission request codes
    const val CAMERA_PERMISSION_REQUEST = 100
    const val LOCATION_PERMISSION_REQUEST = 101
    const val STORAGE_PERMISSION_REQUEST = 102
    const val ALL_PERMISSIONS_REQUEST = 200

    // Required permissions for CrowdShift app
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Check if a single permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in an array are granted
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }

    /**
     * Check if location permissions are granted
     */
    fun areLocationPermissionsGranted(context: Context): Boolean {
        return arePermissionsGranted(context, LOCATION_PERMISSIONS)
    }

    /**
     * Check if fine location permission is granted
     */
    fun isFineLocationPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Check if storage permissions are granted
     */
    fun areStoragePermissionsGranted(context: Context): Boolean {
        return arePermissionsGranted(context, STORAGE_PERMISSIONS)
    }

    /**
     * Check if all required permissions for the app are granted
     */
    fun areAllRequiredPermissionsGranted(context: Context): Boolean {
        return arePermissionsGranted(context, REQUIRED_PERMISSIONS)
    }

    /**
     * Get list of permissions that are not granted
     */
    fun getDeniedPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { !isPermissionGranted(context, it) }
    }

    /**
     * Request permissions from Activity
     */
    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Request permissions from Fragment
     */
    fun requestPermissions(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: Int
    ) {
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(activity: Activity) {
        requestPermissions(activity, CAMERA_PERMISSIONS, CAMERA_PERMISSION_REQUEST)
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions(activity: Activity) {
        requestPermissions(activity, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST)
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(activity: Activity) {
        requestPermissions(activity, STORAGE_PERMISSIONS, STORAGE_PERMISSION_REQUEST)
    }

    /**
     * Request all required permissions
     */
    fun requestAllRequiredPermissions(activity: Activity) {
        requestPermissions(activity, REQUIRED_PERMISSIONS, ALL_PERMISSIONS_REQUEST)
    }

    /**
     * Check if permission should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if any permission in array should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { shouldShowRequestPermissionRationale(activity, it) }
    }

    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: (requestCode: Int, permissions: Array<out String>) -> Unit,
        onPermissionDenied: (requestCode: Int, permissions: Array<out String>, deniedPermissions: List<String>) -> Unit
    ) {
        if (permissions.isEmpty()) return

        val deniedPermissions = mutableListOf<String>()

        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
            }
        }

        if (deniedPermissions.isEmpty()) {
            onPermissionGranted(requestCode, permissions)
        } else {
            onPermissionDenied(requestCode, permissions, deniedPermissions)
        }
    }

    /**
     * Get user-friendly permission name
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (Read)"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage (Write)"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get user-friendly explanation for permission
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera access is needed to scan QR codes for quick login and card verification."
            Manifest.permission.ACCESS_FINE_LOCATION -> "Precise location access is needed for accurate navigation and finding nearby services."
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Location access is needed to show nearby services and traffic information."
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage access is needed to save and load app data."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage access is needed to save app data and cache."
            else -> "This permission is required for the app to function properly."
        }
    }
}