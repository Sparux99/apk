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
        versionCode = 2
        versionName = "2.0"

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

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") } // لإضافة مكتبات من GitHub مثل DoubleTapPlayerView
}

dependencies {
    // Material Design Components
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Core KTX
    implementation("androidx.core:core-ktx:1.12.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer:2.19.0")

    // DoubleTapPlayerView (من JitPack)
    implementation("com.github.vkay94:DoubleTapPlayerView:master-SNAPSHOT")

    // Unit testing
    testImplementation("junit:junit:4.13.2")

    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
