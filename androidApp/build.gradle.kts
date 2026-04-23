plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.cross.sync"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "com.cross.sync.androidApp"
        versionCode = 1
        versionName = "1.0.0"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.activityCompose)
    implementation(libs.ui)
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.foundation)
    implementation(libs.runtime)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.napier)

    implementation(projects.shared.features.client)
    implementation(projects.shared.features.clipboard.data)
    implementation(projects.shared.features.clipboard.domain)
    implementation(projects.shared.features.syncing.data)
    implementation(projects.shared.core.ui)

}
