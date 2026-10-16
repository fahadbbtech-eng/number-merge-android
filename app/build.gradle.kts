plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bluebell.numbermerge"
    compileSdk = 36

    defaultConfig {
        // Play Store id. Never change after the first upload.
        applicationId = "in.bbtech.numbermerge"
        minSdk = 24
        targetSdk = 36
        // Every GitHub build gets the next number, so Google Play always accepts the update.
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = run
        versionName = "1.0.$run"
    }

    // Signing key lives only in GitHub secrets (never in this repo).
    val ks = System.getenv("ANDROID_KEYSTORE_FILE")
    signingConfigs {
        create("release") {
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "bbtech"
                keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ks != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
