package com.roulette.tracker.camera

data class FrameAnalyzerConfig(
    val targetFps: Int = 30,
    val processingInterval: Long = (1000 / targetFps).toLong(),
    val colorConversion: ColorConversion = ColorConversion.YUV2BGR
) {
    enum class ColorConversion {
        YUV2BGR,
        YUV2RGB,
        NONE
    }
} 