plugins {
    id("com.android.application")
    kotlin("android") version "1.9.10"
}

android {
    namespace = "com.amine.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amine.player"
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
    
    // Google's Media3 (ExoPlayer) for video playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // RecyclerView to show video list
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Unit testing
    testImplementation("junit:junit:4.13.2")

    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
