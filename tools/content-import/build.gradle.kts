plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

dependencies {
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
}

application { mainClass.set("ContentImportKt") }

tasks.test { useJUnitPlatform() }

// Push only content that passed the content tests (format, ids, TR+EN, no drinking words).
tasks.named<JavaExec>("run") {
    dependsOn(":shared:testAndroidHostTest")
    workingDir = rootDir
}
