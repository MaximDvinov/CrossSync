plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.cross.sync.syncing.network"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
    }

    jvm()

    sourceSets {
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
            implementation("ch.qos.logback:logback-classic:1.5.6")
            implementation(libs.ktor.server.auth.jwt)
            implementation(libs.slf4j.simple)
        }
    }
}
