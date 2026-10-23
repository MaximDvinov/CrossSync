plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.cross.sync.clipboard.di"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    jvm()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(libs.koin.core)

            implementation(projects.shared.features.clipboard.data)
            implementation(projects.shared.core.db)
            api(projects.shared.features.clipboard.viewmodel)
            implementation(libs.koin.core.viewmodel)
            api(projects.shared.features.clipboard.domain)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
