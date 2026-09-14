plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.cross.sync.notifications.domain"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
        jvmMain.dependencies { implementation(libs.kotlinx.coroutines.swing) }
    }
}
