package com.udacity.project4.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

//Check if foreground location permission already granted
private fun checkForegroundPermissionGranted(context: Context): Boolean {
    return (
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                    context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
            )
}