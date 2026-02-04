import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load secrets.properties file
val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secretsProperties.load(it) }
}

// Helper function to safely retrieve values (returns empty string if key does not exist)
fun getSecret(key: String): String {
    return "\"${secretsProperties.getProperty(key, "")}\""
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.github.konradcz2001.kinootv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.konradcz2001.kinootv"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        // Retrieve data from secrets.properties
        buildConfigField("String", "FIREBASE_LOGIN", getSecret("FIREBASE_LOGIN"))
        buildConfigField("String", "FIREBASE_PASSWORD", getSecret("FIREBASE_PASSWORD"))
        buildConfigField("String", "YOUTUBE_API_KEY", getSecret("YOUTUBE_API_KEY"))
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Enable BuildConfig feature
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Define flavor dimension for different device types
    flavorDimensions += "deviceType"

    productFlavors {
        // FLAVOR 1: Standard Google TV devices
        create("GoogleTV") {
            dimension = "deviceType"
            isDefault = true
            buildConfigField("String", "APP_LOGIN", getSecret("GOOGLE_TV_APP_LOGIN"))
            buildConfigField("String", "APP_PASSWORD", getSecret("GOOGLE_TV_APP_PASSWORD"))
            buildConfigField("String", "FIREBASE_DB_URL", getSecret("GOOGLE_TV_DB_URL"))
        }

        // FLAVOR 2: Fire Stick devices
        create("FireTV") {
            dimension = "deviceType"
            buildConfigField("String", "APP_LOGIN", getSecret("FIRE_TV_APP_LOGIN"))
            buildConfigField("String", "APP_PASSWORD", getSecret("FIRE_TV_APP_PASSWORD"))
            buildConfigField("String", "FIREBASE_DB_URL", getSecret("FIRE_TV_DB_URL"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.coil.compose)

    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.android.youtube.player)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}