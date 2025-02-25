@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var confidenceThreshold: Float
        get() = prefs.getFloat(KEY_CONFIDENCE_THRESHOLD, DEFAULT_CONFIDENCE_THRESHOLD)
        set(value) = prefs.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, value).apply()

    var predictionDelay: Int
        get() = prefs.getInt(KEY_PREDICTION_DELAY, DEFAULT_PREDICTION_DELAY)
        set(value) = prefs.edit().putInt(KEY_PREDICTION_DELAY, value).apply()

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "roulette_tracker_settings"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_PREDICTION_DELAY = "prediction_delay"
        private const val KEY_DEBUG_MODE = "debug_mode"
        
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        private const val DEFAULT_PREDICTION_DELAY = 2
        private const val DEFAULT_DEBUG_MODE = false
    }
} 