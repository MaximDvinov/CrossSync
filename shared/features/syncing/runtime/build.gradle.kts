plugins { alias(libs.plugins.kotlin.multiplatform) }
kotlin {
    jvm()
    macosArm64()
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.features.syncing.domain)
            api(projects.shared.features.clipboard.domain)
            api(projects.shared.features.notifications.domain)
            api(projects.shared.features.setting.domain)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
