package com.roulette.tracker.ui.tracking

data class FrameProviderConfig(
    val frameRate: Int = 30,
    val resolution: Resolution = Resolution.HD,
    val useHardwareAcceleration: Boolean = true
) {
    enum class Resolution(val width: Int, val height: Int) {
        HD(1280, 720),
        FULL_HD(1920, 1080),
        UHD(3840, 2160)
    }
} 