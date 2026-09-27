plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// app/google-services.json is git-ignored (public repo). Clean clones build in fake AI mode.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
}
val aiMode = if (hasGoogleServices && providers.gradleProperty("coinlens.ai").orNull != "fake") "firebase" else "fake"
val admobAppId = providers.gradleProperty("admob.appId")
    .orElse(providers.environmentVariable("ADMOB_APP_ID"))
    .getOrElse("ca-app-pub-3940256099942544~3347511713")
val releaseStoreFile = providers.environmentVariable("COINLENS_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("COINLENS_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("COINLENS_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("COINLENS_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
require(releaseSigningValues.all { it.isNullOrBlank() } || releaseSigningValues.all { !it.isNullOrBlank() }) {
    "Set all four COINLENS signing environment variables, or leave all unset."
}

android {
    namespace = "app.novushq.coinlens"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.novushq.coinlens"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "AI_MODE", "\"$aiMode\"")
    }

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (!releaseStoreFile.isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:identify"))
    implementation(project(":core:ai"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:capture"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:result"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:home"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:collection"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:share"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:onboarding"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:paywall"))
}

// Factory: feature modules (deterministic, do not hand-edit)
dependencies {
    implementation(project(":feature:settings"))
}
