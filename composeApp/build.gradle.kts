import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

// Path to the pre-built llama.cpp XCFramework (built via scripts/build_llama_ios.sh)
val llamaXcfDir = rootProject.file("third_party/llama-ios.xcframework")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // ── iOS targets ────────────────────────────────────────────────────────────
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        // Pick the correct XCFramework slice for each target
        val xcfSlice = when (target.name) {
            "iosArm64"          -> "ios-arm64"
            else                -> "ios-arm64_x86_64-simulator" // iosX64 + iosSimulatorArm64
        }

        target.compilations.getByName("main") {
            cinterops {
                val llama by creating {
                    defFile = file("src/iosMain/cinterop/llama.def")
                    packageName = "com.nfscan.cinterop.llama"
                    // Headers are inside the XCFramework slice
                    includeDirs("${llamaXcfDir.absolutePath}/$xcfSlice/Headers")
                }
            }
        }

        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Link the merged static library and required Apple frameworks
            linkerOpts(
                "-L${llamaXcfDir.absolutePath}/$xcfSlice",
                "-lllama",
                "-framework", "Metal",
                "-framework", "MetalPerformanceShaders",
                "-framework", "Accelerate",
            )
        }
    }

    // ── Source sets ────────────────────────────────────────────────────────────
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.sqldelight)
            implementation(libs.bundles.lifecycle)
            implementation(libs.navigation.compose)

            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            // ML Kit – text recognition (OCR) and PDF rendering
            implementation(libs.mlkit.text.recognition)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.nfscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nfscan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("NFScanDatabase") {
            packageName.set("com.nfscan.data.db")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}
