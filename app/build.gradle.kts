plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    kotlin("kapt")
}

buildscript {
    dependencies {
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${Versions.ksp}")
    }
}

android {
    namespace = "com.roulette.tracker"
    compileSdk = Versions.compileSdk
    buildToolsVersion = Versions.buildToolsVersion

    defaultConfig {
        applicationId = "com.roulette.tracker"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        ndkVersion = "26.1.10909125"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
    }

    ksp {
        arg("dagger.fastInit", "true")
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    kapt {
        correctErrorTypes = true
        arguments {
            arg("dagger.fastInit", "true")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin", "src/main/java")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin", "src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/kotlin", "src/androidTest/java")
        }
    }

    configurations.all {
        resolutionStrategy {
            exclude(group = "org.tensorflow.lite.support", module = "litert-support-api")
        }
    }
    
    lint {
        warning += "DuplicateNamespace"
    }
}

dependencies {
    // AndroidX Core
    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.appCompat)
    
    // Material Design
    implementation(Deps.UI.material)
    
    // Lifecycle
    implementation(Deps.AndroidX.Lifecycle.runtime)
    implementation(Deps.AndroidX.Lifecycle.viewModel)
    
    // Hilt
    implementation(Deps.Hilt.android)
    kapt(Deps.Hilt.compiler)
    
    // Hilt Extensions
    implementation(Deps.Hilt.work)
    implementation(Deps.Hilt.navigationCompose)
    kapt(Deps.Hilt.workCompiler)
    
    // Worker
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Coroutines für Worker
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    
    // Navigation
    implementation(Deps.Navigation.fragment)
    implementation(Deps.Navigation.ui)
    
    // CameraX
    implementation(Deps.AndroidX.Camera.core)
    implementation(Deps.AndroidX.Camera.camera2)
    implementation(Deps.AndroidX.Camera.lifecycle)
    implementation(Deps.AndroidX.Camera.view)
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // TensorFlow
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    
    // OCR - Verwende die lokale Tesseract-Bibliothek oder eine alternative OCR-Lösung
    // Temporär auskommentiert bis wir eine Alternative haben
    // implementation("com.github.adaptech-cz.Tesseract4Android:tesseract4android:4.8.0")
    
    // Tests
    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.androidxJunit)
    androidTestImplementation(Deps.Test.espresso)

    kspTest(Deps.Hilt.compiler)
    kspAndroidTest(Deps.Hilt.compiler)

    coreLibraryDesugaring(Deps.Tools.desugar)

    // Kotlin
    implementation(Deps.Kotlin.bom)
    implementation(Deps.Kotlin.stdlib)
    implementation(Deps.Kotlin.coroutinesAndroid)
    implementation(Deps.Kotlin.coroutinesCore)

    // OpenCV - Verwenden Sie das lokale Modul
    implementation(project(":opencv"))

    // Compose Dependencies
    implementation(platform("androidx.compose:compose-bom:${Versions.composeBom}"))
    androidTestImplementation(platform("androidx.compose:compose-bom:${Versions.composeBom}"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Stelle sicher, dass die Kotlin-Version korrekt ist
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}"))

    // Add javax.inject dependency
    implementation("javax.inject:javax.inject:1")
} 