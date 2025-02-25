package com.roulette.tracker.camera

import org.opencv.core.Mat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrameAnalyzerFactory @Inject constructor() {
    
    fun create(
        onFrameAnalyzed: (Mat) -> Unit,
        config: FrameAnalyzerConfig = FrameAnalyzerConfig()
    ): FrameAnalyzer {
        return FrameAnalyzer(onFrameAnalyzed, config)
    }
} 