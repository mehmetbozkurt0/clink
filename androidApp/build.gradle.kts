plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.mehmetbozkurt.clink"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mehmetbozkurt.clink"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    // Debug builds are signed with Clink's own debug key (secrets/, git-ignored), so its SHA-1 registered in Firebase
    // is the same on every machine that has the file. Without the file, the machine's default debug key is used.
    signingConfigs {
        getByName("debug") {
            val clinkDebugKey = rootProject.file("secrets/clink-debug.keystore")
            if (clinkDebugKey.exists()) {
                storeFile = clinkDebugKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
