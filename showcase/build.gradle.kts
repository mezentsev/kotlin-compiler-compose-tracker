plugins {
    alias(libs.plugins.android.application).apply(true)
    alias(libs.plugins.kotlin.android).apply(true)
    alias(libs.plugins.compose.compiler).apply(true)
    kotlin("kapt")

    id("pro.mezentsev.tracker.compose")
}

android {
    namespace = "pro.mezentsev.tracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "pro.mezentsev.tracker"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
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

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(path = ":tracker-profiler"))

    kapt(libs.daggerCompiler)
    implementation(libs.dagger)
    implementation(libs.javaXInject)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.viewPager2)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.material)

    implementation(libs.timber)
    implementation(libs.coroutines)

    implementation(libs.bundles.compose)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    testImplementation(libs.junit.core)
}

composeCompiler {
    enableStrongSkippingMode = true

    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}
