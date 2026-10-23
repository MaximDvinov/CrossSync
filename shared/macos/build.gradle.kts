plugins { alias(libs.plugins.kotlin.multiplatform) }

kotlin {
    macosArm64()
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "CrossSyncShared"
            isStatic = true
            export(projects.shared.features.clipboard.viewmodel)
            export(projects.shared.features.setting.viewmodel)
            export(projects.shared.features.notifications.viewmodel)
            export(projects.shared.features.clipboard.domain)
            export(projects.shared.features.setting.domain)
            export(projects.shared.features.syncing.domain)
            export(projects.shared.features.notifications.domain)
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.features.clipboard.viewmodel)
            api(projects.shared.features.setting.viewmodel)
            api(projects.shared.features.notifications.viewmodel)
            api(projects.shared.features.clipboard.domain)
            api(projects.shared.features.setting.domain)
            api(projects.shared.features.syncing.domain)
            api(projects.shared.features.notifications.domain)
            implementation(projects.shared.features.clipboard.di)
            implementation(projects.shared.features.clipboard.data)
            implementation(projects.shared.features.syncing.data)
            implementation(projects.shared.features.syncing.network)
            implementation(projects.shared.features.syncing.runtime)
            implementation(projects.shared.features.notifications.di)
            implementation(projects.shared.core.db)
            implementation(libs.koin.core)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
