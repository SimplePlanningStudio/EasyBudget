plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.simplebudget"
    compileSdk = 35

    defaultConfig {
        applicationId = "gplx.simple.budgetapp"
        minSdk = 26
        targetSdk = 35
        compileSdk = 35
        versionCode = 65
        versionName = "3.3.6"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
    }

    // Define product flavors
    flavorDimensions.add("default")
    productFlavors {
        create("prod") {
            dimension = "default"
            applicationId = "gplx.simple.budgetapp"
            resValue("string", "app_name_simple_budget", "Simple Budget")
            resValue("string", "app_name_simplebudget_title", "SimpleBudget")
        }
        create("dev") {
            dimension = "default"
            applicationId = "gplx.simple.budgetapp.dev"
            resValue("string", "app_name_simple_budget", "Dev Simple Budget")
            resValue("string", "app_name_simplebudget_title", "Dev SimpleBudget")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true

            // Ensure that both prod and dev flavors can be used in debug
            firebaseCrashlytics {
                mappingFileUploadEnabled = false
            }

            // Apply the same configurations for debug builds
            buildConfigField("boolean", "DEBUG_LOG", "true")
            buildConfigField("boolean", "CRASHLYTICS_ACTIVATED", "true")
            buildConfigField("boolean", "ANALYTICS_ACTIVATED", "true")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false // You might want to set this to false for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Apply configurations for release builds
            buildConfigField("boolean", "DEBUG_LOG", "false")
            buildConfigField("boolean", "CRASHLYTICS_ACTIVATED", "true")
            buildConfigField("boolean", "ANALYTICS_ACTIVATED", "true")
        }
    }
    // Ensure that both flavors can be installed in debug mode
    applicationVariants.all {
        if (buildType.name == "debug") {
            outputs.forEach { output ->
                val outputImpl =
                    output as com.android.build.gradle.internal.api.BaseVariantOutputImpl
                outputImpl.outputFileName = "app-${flavorName}-debug.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":ezpermission"))
    implementation(project(":MPChartLib"))
    implementation(project(":caldroid"))

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    //KTX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Work manager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.work:work-gcm:2.10.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    //Google Ads
    implementation("com.google.android.gms:play-services-ads:24.0.0")

    //In App Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    //Room
    implementation("androidx.room:room-ktx:2.6.1")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // SSP, SDP
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    //gson
    implementation("com.google.code.gson:gson:2.11.0")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Koin DI
    implementation("io.insert-koin:koin-android:3.5.6")

    //Lottie
    implementation("com.airbnb.android:lottie:6.4.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.ump:user-messaging-platform")
    implementation("com.google.firebase:firebase-inappmessaging-display")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.getbase:floatingactionbutton:1.10.1")
    implementation("me.relex:circleindicator:2.1.6@aar")
    implementation("androidx.viewpager:viewpager:1.1.0")
    api("com.darwinsys:hirondelle-date4j:1.5.1")

    //https://developer.android.com/studio/write/java8-support#library-desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
