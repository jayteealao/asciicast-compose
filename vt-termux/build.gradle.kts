plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "uk.adedamola.asciicast.vt.termux"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":vt-api"))

    // TODO: Add Termux terminal-emulator dependency when implementing
    // Note: Check licensing requirements (GPLv3)
    // implementation("com.termux:terminal-emulator:...")

    testImplementation("junit:junit:4.13.2")
}
