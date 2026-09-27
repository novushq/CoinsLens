plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.novushq.coinlens.ai"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

// AI_MODE is "firebase" only when a Firebase project is configured AND the
// build was not forced to fake. This repo ships without google-services.json,
// so local builds, CI and tests always run the deterministic fake engine.
val hasGoogleServices = rootProject.file("app/google-services.json").exists()
val forceFake = providers.gradleProperty("coinlens.ai").orNull == "fake"
android.defaultConfig.buildConfigField(
    "String", "AI_MODE", "\"${if (hasGoogleServices && !forceFake) "firebase" else "fake"}\"",
)

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":core:identify"))
    implementation(project(":core:domain"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}
