plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.cross.sync.syncing.network"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    jvm()
    macosArm64()

    applyDefaultHierarchyTemplate {
        common {
            group("desktop") {
                withJvm()
                group("macos") { withMacosArm64() }
            }
        }
    }
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.websockets)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.auth)
            }
        }
        macosMain.dependencies {
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.client.darwin)
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(libs.multiplatformSettings)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.json)

            implementation(libs.napier)

            implementation(projects.shared.features.syncing.domain)
            implementation(projects.shared.features.clipboard.domain)
            implementation(projects.shared.features.notifications.domain)

        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)

            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.call.logging)
            implementation(libs.ktor.server.auth)
            implementation(libs.logback.classic)
            implementation(libs.ktor.server.auth.jwt)
            implementation(libs.slf4j.simple)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
