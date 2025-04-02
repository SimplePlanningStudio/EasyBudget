plugins {
    id("com.android.library")
}


android {
    namespace = "com.caldroid"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        compileSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.viewpager:viewpager:1.1.0")
    api("com.darwinsys:hirondelle-date4j:1.5.1")
}
