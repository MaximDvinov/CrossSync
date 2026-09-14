plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.0.0").get()
val appVersionCode = providers.gradleProperty("appVersionCode").orElse("1").get().toInt()

android {
    namespace = "com.cross.sync"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 37

        applicationId = "com.cross.sync.androidApp"
        versionCode = appVersionCode
        versionName = appVersion
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
    implementation(projects.shared.features.syncing.domain)
    implementation(projects.shared.features.notifications.data)
    implementation(projects.shared.features.notifications.domain)
    implementation(projects.shared.features.setting.domain)
    implementation(projects.shared.core.ui)

}
