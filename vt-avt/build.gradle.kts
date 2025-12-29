plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "uk.adedamola.asciicast.vt.avt"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Target ABIs for NDK build
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
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
            jniLibs.srcDirs("android/src/main/jniLibs")
        }
    }
}

dependencies {
    api(project(":vt-api"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

// TODO: Add Rust build tasks using cargo-ndk
// tasks.register<Exec>("buildRustLibs") {
//     workingDir = file("rust")
//     commandLine("cargo", "ndk", "--target", "armeabi-v7a", "--target", "arm64-v8a", "--target", "x86_64", "--", "build", "--release")
// }
