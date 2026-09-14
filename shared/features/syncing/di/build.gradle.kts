plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.cross.sync.syncing.di"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.multiplatformSettings)

            implementation(projects.shared.features.syncing.network)
            api(projects.shared.features.syncing.domain)
            implementation(projects.shared.features.syncing.data)
            api(projects.shared.features.syncing.presentation)
            implementation(projects.shared.core.db)
            implementation(projects.shared.features.clipboard.domain)
            implementation(projects.shared.features.notifications.domain)
        }

        androidMain.dependencies {
        }

        jvmMain.dependencies {
        }
    }
}
