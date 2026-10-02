plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.cross.sync.syncing.data"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(projects.shared.features.syncing.domain)
            implementation(projects.shared.features.clipboard.domain)
            implementation(projects.shared.core.db)
            implementation(projects.shared.features.syncing.network)
            implementation(projects.shared.features.notifications.domain)

        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core)
            implementation(libs.androidx.lifecycle.service)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
