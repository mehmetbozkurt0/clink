import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "com.mehmetbozkurt.clink.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        androidResources { enable = true }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.mehmetbozkurt.clink.resources"
}
