    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.compose)
        alias(libs.plugins.kotlin.serialization)

        alias(libs.plugins.hilt)
        alias(libs.plugins.ksp)
        alias(libs.plugins.sqldelight)
    }

    android {
        namespace = "com.example.siliguide"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }

        defaultConfig {
            applicationId = "com.example.siliguide"
            minSdk = 24
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        buildFeatures {
            compose = true
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    // SQLDelight Local Database Configuration
    sqldelight {
        databases {
            create("SiliGuideDatabase") {
                packageName.set("com.example.siliguide.data.local")

                // UPGRADED: Forces a fully modern engine parsing model for Postgres-style queries
                dialect("app.cash.sqldelight:sqlite-3-35-dialect:2.3.2")
            }
        }
    }

    dependencies {
        // Core Android & Compose UI
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.material.icons.extended)
        implementation(libs.androidx.lifecycle.viewmodel.compose)

        // Navigation, UI Utilities & Image Loading
        implementation(libs.androidx.navigation.compose)
        implementation(libs.coil.compose)
        implementation(libs.accompanist.permissions)

        // Camera & Image Processing (CameraX)
        implementation(libs.androidx.camera.core)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle)
        implementation(libs.androidx.camera.view)

        // For QR Code Scanning and Decoding
        implementation(libs.barcode.scanning)

        // Machine Learning (TFLite)
        implementation(libs.tensorflow.lite)
        //implementation(libs.tensorflow.lite.support)

        // Authentication (Credential Manager)
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)

        // Backend & Cloud (Supabase)
        implementation(libs.supabase.auth)
        implementation(libs.supabase.postgrest)
        implementation(libs.supabase.storage)
        implementation(libs.supabase.realtime)

        // IoT Communication Ktor OkHttp
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.okhttp)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)

        // Local Storage & Offline Sync
        implementation(libs.sqldelight.android.driver)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.androidx.work.runtime.ktx)

        // Dependency Injection (Hilt - NEW)
        implementation(libs.hilt.android)
        implementation(libs.androidx.compose.foundation)
        ksp(libs.hilt.compiler) // Note: KSP is used instead of kapt to improve build speeds
        implementation(libs.androidx.hilt.navigation.compose)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
