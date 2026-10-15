// Test double for the emulator: pretends to be Meet (same package) and plays scripted captions.
plugins {
    id("com.android.application")
}

android {
    namespace = "dev.octrow.fakemeet"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.google.android.apps.tachyon"
        minSdk = 30
        targetSdk = 36
    }
}
