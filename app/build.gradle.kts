plugins {
    id("com.android.application")
}

android {
    namespace = "dev.octrow.meetcaptions"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.octrow.meetcaptions"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Release signing comes from CI secrets (see .github/workflows/release.yml); without them the APK is unsigned.
    System.getenv("SIGNING_KEYSTORE")?.let { keystore ->
        signingConfigs.create("release") {
            storeFile = file(keystore)
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
