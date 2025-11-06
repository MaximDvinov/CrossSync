import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.hot.reload)
}

dependencies {
    implementation(compose.ui)
    implementation(libs.jna)
    implementation(libs.jnativehook)
    implementation(libs.composeIcons.feather)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    implementation(projects.shared.features.clipboard.data)
    implementation(projects.shared.features.clipboard.di)
    implementation(projects.shared.core.ui)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs += listOf("-Dapple.awt.UIElement=true")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CrossSync"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "com.cross.sync.desktopApp"
            }
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass = "MainKt"
}
