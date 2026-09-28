plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val revenueCatApiKey = providers.gradleProperty("revenuecat.apiKey")
    .orElse(providers.environmentVariable("REVENUECAT_API_KEY"))
    .getOrElse("")
val rewardedAdUnitId = providers.gradleProperty("admob.rewardedUnitId")
    .orElse(providers.environmentVariable("ADMOB_REWARDED_UNIT_ID"))
    .getOrElse("")
val termsUrl = providers.gradleProperty("coinlens.termsUrl")
    .orElse(providers.environmentVariable("COINLENS_TERMS_URL"))
    .getOrElse("")
val privacyUrl = providers.gradleProperty("coinlens.privacyUrl")
    .orElse(providers.environmentVariable("COINLENS_PRIVACY_URL"))
    .getOrElse("")

android {
    namespace = "app.novushq.coinlens.feature.paywall"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatApiKey\"")
        buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$rewardedAdUnitId\"")
        buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
        buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
 buildFeatures {
 compose = true
 buildConfig = true
 }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.androidx.compose)
 implementation(libs.coil.compose)
 implementation(libs.androidx.core.ktx)
    implementation(libs.purchases)
    implementation(libs.play.services.ads)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
