plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.cross.sync.syncing.di"
        compileSdk = 36
        minSdk = 23
        androidResources.enable = true
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)

            implementation(projects.shared.features.syncing.network)
            api(projects.shared.features.syncing.domain)
            implementation(projects.shared.features.syncing.data)
            implementation(projects.shared.features.clipboard.db)
            implementation(projects.shared.features.clipboard.domain)
        }

        androidMain.dependencies {
        }

        jvmMain.dependencies {
        }
    }
}
