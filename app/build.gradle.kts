plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.ksp)


}

android {
    namespace = "com.example.yrmultimediaco.sos"
    compileSdk = 36
//    compileSdk {
//        version = release(36)
//    }

    defaultConfig {
        applicationId = "com.example.yrmultimediaco.sos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    //added by Yash  for Room
    kapt {
        correctErrorTypes = true
        useBuildCache = false
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Room dependencies
    implementation("androidx.room:room-runtime:2.8.4")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.8.4") // Coroutine support

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
}