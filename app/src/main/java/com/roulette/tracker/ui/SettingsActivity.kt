package com.roulette.tracker

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.roulette.tracker.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadSettings()
    }
    
    private fun loadSettings() {
        // Lade gespeicherte Einstellungen
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.wheelDiameter.setText(prefs.getString("wheelDiameter", ""))
        binding.ballDiameter.setText(prefs.getString("ballDiameter", ""))
        binding.numberSpacing.setText(prefs.getString("numberSpacing", ""))
        binding.minTrackingFrames.setText(prefs.getString("minTrackingFrames", ""))
        binding.predictionWindow.setText(prefs.getString("predictionWindow", ""))
    }
    
    override fun onPause() {
        super.onPause()
        // Speichere Einstellungen
        getSharedPreferences("settings", Context.MODE_PRIVATE).edit().apply {
            putString("wheelDiameter", binding.wheelDiameter.text.toString())
            putString("ballDiameter", binding.ballDiameter.text.toString())
            putString("numberSpacing", binding.numberSpacing.text.toString())
            putString("minTrackingFrames", binding.minTrackingFrames.text.toString())
            putString("predictionWindow", binding.predictionWindow.text.toString())
            apply()
        }
    }
} 