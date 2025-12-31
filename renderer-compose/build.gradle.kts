plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    namespace = "uk.adedamola.asciicast.renderer"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":vt-api"))
    api(project(":player-core"))

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.material3)

    implementation("androidx.compose.ui:ui-text:1.5.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
