object Versions {
    const val kotlin = "1.9.22"
    const val hilt = "2.55"
    const val hiltWork = "1.1.0"
    const val hiltNavigationCompose = "1.1.0"
    const val androidxHiltCompiler = "1.1.0"
    const val compileSdk = 35
    const val minSdk = 26
    const val targetSdk = 35
    
    const val ksp = "1.9.22-1.0.17"
    
    const val navigation = "2.7.6"
    
    // Korrekte Versionen für SDK 34
    const val androidxCore = "1.12.0"
    const val androidxAppCompat = "1.6.1"
    const val material = "1.11.0"
    const val lifecycleRuntime = "2.7.0"
    const val lifecycleViewModel = "2.7.0"
    const val cameraX = "1.4.1"
    const val room = "2.6.1"
    const val tensorflow = "2.14.0"
    const val tensorflowSupport = "0.4.4"
    const val junit = "4.13.2"
    const val androidxJunit = "1.2.1"
    const val espresso = "3.6.1"
    const val desugar = "2.1.4"
    const val kotlinStdlib = "1.9.22"
    const val coroutines = "1.7.3"
    
    const val buildToolsVersion = "35.0.0"
    const val agp = "8.8.1"
    const val gradle = "8.10.2"
    
    const val composeCompiler = "1.5.8"
    const val composeBom = "2024.02.00"
}

object Deps {
    object AndroidX {
        const val core = "androidx.core:core-ktx:${Versions.androidxCore}"
        const val appCompat = "androidx.appcompat:appcompat:${Versions.androidxAppCompat}"
        
        object Lifecycle {
            const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycleRuntime}"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycleViewModel}"
        }
        
        object Camera {
            const val core = "androidx.camera:camera-core:${Versions.cameraX}"
            const val camera2 = "androidx.camera:camera-camera2:${Versions.cameraX}"
            const val lifecycle = "androidx.camera:camera-lifecycle:${Versions.cameraX}"
            const val view = "androidx.camera:camera-view:${Versions.cameraX}"
        }
    }

    object Hilt {
        const val android = "com.google.dagger:hilt-android:${Versions.hilt}"
        const val compiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
        const val work = "androidx.hilt:hilt-work:${Versions.hiltWork}"
        const val workCompiler = "androidx.hilt:hilt-compiler:${Versions.androidxHiltCompiler}"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigationCompose}"
    }

    object Navigation {
        const val fragment = "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
        const val ui = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
    }

    object UI {
        const val material = "com.google.android.material:material:${Versions.material}"
    }

    object Test {
        const val junit = "junit:junit:${Versions.junit}"
        const val androidxJunit = "androidx.test.ext:junit:${Versions.androidxJunit}"
        const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    }
    
    object Tools {
        const val desugar = "com.android.tools:desugar_jdk_libs:${Versions.desugar}"
    }

    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlinStdlib}"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val bom = "org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}"
    }

    object Compose {
        const val bom = "androidx.compose:compose-bom:${Versions.composeBom}"
        const val ui = "androidx.compose.ui:ui"
        const val uiGraphics = "androidx.compose.ui:ui-graphics"
        const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
        const val material3 = "androidx.compose.material3:material3"
        const val runtime = "androidx.compose.runtime:runtime"
        const val uiTooling = "androidx.compose.ui:ui-tooling"
        const val uiTestManifest = "androidx.compose.ui:ui-test-manifest"
    }
} 