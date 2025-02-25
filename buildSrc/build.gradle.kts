plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("com.android.tools.build:gradle:8.8.1")
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
        cacheDynamicVersionsFor(0, "seconds")
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
        force("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    }
} 