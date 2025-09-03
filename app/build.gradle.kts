plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.notepad"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notepad"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // 添加签名配置
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.keystore")
            storePassword = "notepad123"
            keyAlias = "notepad"
            keyPassword = "notepad123"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
        
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        viewBinding = true
    }

    // 配置App Bundle，不按语言拆分
    bundle {
        language {
            enableSplit = false
        }
    }

    // 配置APK输出文件名
    applicationVariants.all {
        val outputFileName = "轻笺QingNote-v${defaultConfig.versionName}-${defaultConfig.versionCode}-${buildType.name}.apk"
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (output.outputFile.name.endsWith(".apk")) {
                output.outputFileName = outputFileName
            }
        }
    }
    
    // 禁用一些不需要的构建功能以加快构建速度
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    
    // 更新后的打包选项
    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Lifecycle components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    
    // Room components
    implementation(libs.room.runtime)
    implementation(libs.room.common.jvm)
    implementation(libs.room.paging)
    annotationProcessor(libs.room.compiler)
    
    // Paging
    implementation(libs.paging.runtime)
    
    // Image loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    
    // Camera
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    
    // JSON parsing
    implementation(libs.gson)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}