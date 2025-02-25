package com.roulette.tracker.ui.tracking

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import timber.log.Timber

class CameraLifecycleManager @Inject constructor() : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<CameraState>(CameraState.Initialized)
    val state: StateFlow<CameraState> = _state
    
    private var frameProvider: CameraFrameProvider? = null
    
    fun registerFrameProvider(provider: CameraFrameProvider) {
        frameProvider = provider
    }
    
    override fun onResume(owner: LifecycleOwner) {
        try {
            frameProvider?.let { provider ->
                _state.value = CameraState.Starting
                provider.start()
                _state.value = CameraState.Running
            }
        } catch (e: Exception) {
            Timber.e(e, "Error resuming camera")
            _state.value = CameraState.Error(e)
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        try {
            frameProvider?.let { provider ->
                _state.value = CameraState.Stopping
                provider.stop()
                _state.value = CameraState.Stopped
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pausing camera")
            _state.value = CameraState.Error(e)
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        scope.cancel()
        frameProvider = null
    }
}

sealed class CameraState {
    object Initialized : CameraState()
    object Starting : CameraState()
    object Running : CameraState()
    object Stopping : CameraState()
    object Stopped : CameraState()
    data class Error(val exception: Exception) : CameraState()
} 