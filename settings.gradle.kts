pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Spezifische Version für AGP
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application",
                "com.android.library" -> useVersion("8.2.2")
                "org.jetbrains.kotlin.android" -> useVersion("1.9.22")
                "com.google.devtools.ksp" -> useVersion("1.9.22-1.0.17")
                "com.google.dagger.hilt.android" -> useVersion("2.55")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.github.com/opencv/opencv") }
    }
}

rootProject.name = "RouletteTracker"
include(":app")
include(":opencv")

// Diese Feature-Preview erst mal deaktivieren bis wir den Build zum Laufen bekommen
// enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
