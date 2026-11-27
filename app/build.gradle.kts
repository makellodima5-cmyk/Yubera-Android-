plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.yubera.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.yubera.app"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("YUBERA_VERSION_CODE").orNull?.toIntOrNull() ?: 3
        versionName = providers.gradleProperty("YUBERA_VERSION_NAME").orNull ?: "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val storePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val aliasValue = System.getenv("ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("ANDROID_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() && !storePass.isNullOrBlank() && !aliasValue.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = storePass
                keyAlias = aliasValue
                keyPassword = keyPass
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("ANDROID_KEYSTORE_PATH").isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
        }
    }
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
