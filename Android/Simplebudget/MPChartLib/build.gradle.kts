plugins {
    id("com.android.library")
}


android {
    namespace = "com.github.mikephil.charting"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        compileSdk = 35
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}
