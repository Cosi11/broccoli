package com.roulette.tracker.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import org.opencv.android.OpenCVLoader
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import com.roulette.tracker.databinding.ActivityMainBinding
import com.roulette.tracker.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.content.ContextCompat
import android.os.Build
import android.app.AlertDialog
import android.widget.Toast

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i("OpenCV", "OpenCV geladen")
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
        
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            initializeOpenCV()
        }
    }

    private fun initializeOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Interner OpenCV Manager wird initialisiert")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)
        } else {
            Log.d("OpenCV", "OpenCV wurde initialisiert")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            if (shouldShowRequestPermissionRationale()) {
                showPermissionRationaleDialog()
            } else {
                requestPermissions()
            }
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.permissions_explanation)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && !hasRequiredPermissions()) {
            if (shouldShowRequestPermissionRationale()) {
                showPermissionRationaleDialog()
            } else {
                Toast.makeText(
                    this,
                    R.string.permissions_required_error,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } else {
            initializeOpenCV()
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
} 