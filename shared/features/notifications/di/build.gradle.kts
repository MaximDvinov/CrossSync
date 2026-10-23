plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.cross.sync.notifications.di"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }
    jvm()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.shared.core.db)
            implementation(projects.shared.features.notifications.data)
            implementation(projects.shared.features.notifications.domain)
            implementation(projects.shared.features.syncing.domain)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
        jvmMain.dependencies {
            implementation(libs.koin.compose)
            implementation(libs.koin.core.viewmodel)
            implementation(projects.shared.features.notifications.viewmodel)
        }
    }
}
