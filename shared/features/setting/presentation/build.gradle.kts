plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    android {
        namespace = "com.cross.sync.setting.presentation"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.ui)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling)

            implementation(libs.coil.compose)
            implementation(libs.htmlconverter)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatformSettings)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(projects.shared.features.clipboard.domain)
            implementation(projects.shared.features.syncing.domain)
            implementation(projects.shared.features.setting.domain)
            implementation(projects.shared.core.ui)


        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("network.chaintech:qr-kit:${libs.versions.qrKit.get()}") {
                exclude(group = "org.bytedeco")
            }
        }
    }
}
