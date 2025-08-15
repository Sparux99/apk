plugins {
    id("com.android.application")
    kotlin("android") version "1.9.10"
}

android {
    namespace = "com.amine.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amine.app"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Material Design Components
    implementation("com.google.android.material:material:1.10.0")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Core KTX
    implementation("androidx.core:core-ktx:1.12.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // اختبار الوحدة
    testImplementation("junit:junit:4.13.2")

    // اختبار أندرويد
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
