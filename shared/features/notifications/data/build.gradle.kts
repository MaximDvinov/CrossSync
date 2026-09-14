plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.cross.sync.notifications.data"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.shared.core.db)
            implementation(projects.shared.features.notifications.domain)
            implementation(projects.shared.features.syncing.domain)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
        jvmMain.dependencies { implementation(libs.kotlinx.coroutines.swing) }
    }
}
