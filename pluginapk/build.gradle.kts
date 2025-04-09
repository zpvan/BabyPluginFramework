import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.knox.pluginapk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.knox.pluginapk"
        minSdk = 30
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

// 在项目配置评估后添加任务
afterEvaluate {
    android.applicationVariants.all { variant ->
        val variantName = variant.name
        val capitalizedVariantName = variantName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }

        val assembleTask = tasks.named("assemble").get()

        // 创建复制任务
        val copyTask = tasks.register("copy${capitalizedVariantName}ApkToHostAssets") {
            group = "Plugin" // 这会将任务放在名为"Plugin"的组中
            description = "Copies the $variantName APK to hostApp assets directory" // 任务描述

            doLast {
                val targetDir = file("${rootProject.projectDir}/app/src/main/assets")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                // 获取APK输出目录
                val outputDir = variant.outputs.first().outputFile.parentFile
                // 查找APK文件(debug版本)
                val apkFile =
                    outputDir.listFiles()?.find { it.extension == "apk" && it.name.contains("debug") }

                copy {
                    if (apkFile != null) {
                        from(apkFile)
                        println("Copying APK from: ${apkFile.absolutePath}")
                    } else {
                        println("No APK file found in ${outputDir.absolutePath}")
                    }

                    // 定义目标目录(确保目录存在)
                    into(targetDir)
                }

                println("Copied APK to hostApp's assets directory")
            }
        }

        // 确保复制任务在构建完成后执行
        assembleTask.finalizedBy(copyTask)
        true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    compileOnly(project(":pluginlibrary"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}