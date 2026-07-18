import java.util.Properties

// kotlin-android is applied explicitly because android.builtInKotlin=false — see gradle.properties.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing is opt-in: drop a key.properties at the repo root to enable it.
// Absent that, `assembleRelease` still builds an (unsigned) APK for size checks.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.vi5hnu.calculator"
    compileSdk = 37

    defaultConfig {
        // Unchanged from the Flutter app so this installs as an in-place update.
        applicationId = "com.vi5hnu.calculator.calculator"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "3.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = keystoreProperties.getProperty("storeFile")?.let { file(it) }
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            // The real banner unit only ships in release.
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-4715945578201106/1265511015\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            // Google's public test banner unit. Debug MUST NOT use the live unit — tapping your
            // own real ads while developing gets the AdMob account suspended.
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
    }

    buildFeatures {
        compose = true
        // Exposes BuildConfig.BANNER_AD_UNIT_ID above.
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json",
        )
    }

    kotlin {
        jvmToolchain(17)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Compose UI tests run on the JVM under Robolectric rather than needing a device, so
        // they stay part of the normal `testDebugUnitTest` run.
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    // Lets Room verify queries at compile time and emit schemas for future migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
