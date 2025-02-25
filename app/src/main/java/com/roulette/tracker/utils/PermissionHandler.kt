package com.roulette.tracker.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

class PermissionHandler(private val activity: Activity) {
    
    fun checkAndRequestPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onSuccess()
                } else {
                    onFailure()
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }
} 