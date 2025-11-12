plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutLibraries)
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            // 启用 Compose 报告
            "-P=plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$layout.buildDirectory/compose_reports",
            "-P=plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$layout.buildDirectory/compose_metrics"
        )
    }
}

android {
    namespace = "com.ling.box"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ling.box"
        minSdk = 28
        targetSdk = 36
        versionCode = 24
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // 只保留中文资源，减少 APK 体积
    androidResources {
        localeFilters += listOf("zh", "zh-rCN")
    }

    signingConfigs {
        create("release") {
            // 从环境变量读取密钥信息（用于 CI/CD）
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            
            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                val keystore = file(keystoreFile)
                if (keystore.exists()) {
                    storeFile = keystore
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // 推荐：启用资源收缩
            isShrinkResources = true
            isDebuggable = false
            // 如果配置了签名且密钥文件存在，则使用签名配置
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX Core 基础库
    implementation(libs.androidx.core.ktx)

    // AndroidX Activity 和生命周期管理
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose 相关依赖 (通过 Compose BOM 进行版本管理)
    // BOM 版本
    implementation(platform(libs.androidx.compose.bom))
    // Material 图标、Material3
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    // implementation(libs.androidx.compose.ui.tooling.preview)

    // 第三方库
    // Kotlin 序列化
    // Kotlin Serialization JSON 库
    implementation(libs.kotlinx.serialization.json)

    // 网络请求
    // OkHttp 核心库
    implementation(libs.okhttp)

    // AboutLibraries Compose UI
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.androidx.compose.foundation.layout)

    // 测试依赖
    testImplementation(libs.junit)

    // AndroidX 测试依赖
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 测试 BOM 版本
    androidTestImplementation(platform(libs.androidx.compose.bom))
    // Compose UI 测试 JUnit4
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug 依赖
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}