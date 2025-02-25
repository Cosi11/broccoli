plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.opencv"
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            aidl.srcDirs("src/main/aidl")
            manifest.srcFile("src/main/AndroidManifest.xml")
            jniLibs.srcDirs("src/main/jniLibs")
            res.srcDir("src/main/res")
        }
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    
    ndkVersion = "25.2.9519653"

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    tasks.withType<com.android.build.gradle.tasks.AidlCompile>().configureEach {
        outputs.dir(layout.buildDirectory.dir("generated/aidl_source_output_dir/${this.name}"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf(
        "-Xlint:-options",
        "-Xmaxerrs", "500"
    ))
    options.encoding = "UTF-8"
}

tasks.withType<JavaCompile>().configureEach {
    exclude("**/examples/**", "**/samples/**")
}

tasks.withType<JavaCompile>().configureEach {
    doFirst {
        val aidlDir = project.buildDir.resolve("generated/aidl_source_output_dir")
        if (aidlDir.exists()) {
            aidlDir.walk().filter { it.isFile && it.extension == "java" }.forEach { file ->
                val content = file.readText()
                val cleaned = content.replace("\\", "/")
                file.writeText(cleaned)
            }
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")
    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.appCompat)
    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.androidxJunit)
    androidTestImplementation(Deps.Test.espresso)
} 