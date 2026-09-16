import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.5.1").get()

val desktopMainClass = "MainKt"
val desktopJvmArgs = listOf(
    "-Dapple.awt.UIElement=true",
    "-Dskiko.gpu.resourceCacheLimit=50",
    "-Djava.net.preferIPv4Stack=true"
)

val macNotificationBridgeSource = layout.projectDirectory.file(
    "native/macos/CrossSyncNotificationBridge.swift",
)
val macNotificationBridge = layout.buildDirectory.file(
    "generated/macos-notification-bridge/macos/libCrossSyncNotificationBridge.dylib",
)

abstract class CompileMacNotificationBridgeTask : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun compile() {
        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        execOperations.exec {
            commandLine(
                "xcrun",
                "swiftc",
                "-emit-library",
                source.get().asFile.absolutePath,
                "-framework",
                "AppKit",
                "-framework",
                "UserNotifications",
                "-o",
                outputFile.absolutePath,
            )
        }.assertNormalExitValue()
    }
}

val compileMacNotificationBridge = tasks.register<CompileMacNotificationBridgeTask>(
    "compileMacNotificationBridge",
) {
    source.set(macNotificationBridgeSource)
    output.set(macNotificationBridge)
    onlyIf { System.getProperty("os.name").contains("mac", ignoreCase = true) }
}

@DisableCachingByDefault(because = "Spawns a long-lived UI process and returns immediately.")
abstract class RunDetachedTask : DefaultTask() {
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Internal
    abstract val workingDir: DirectoryProperty

    @TaskAction
    fun runDetached() {
        val runtimeClasspath = classpath.files.joinToString(File.pathSeparator) { it.absolutePath }

        val javaHome = File(System.getProperty("java.home"))
        val javaCandidate = File(javaHome, "bin/java")
        val javaExec = when {
            javaCandidate.exists() -> javaCandidate
            File(javaCandidate.absolutePath + ".exe").exists() -> File(javaCandidate.absolutePath + ".exe")
            else -> error("Cannot find java executable under java.home: $javaHome")
        }

        val cmd = buildList {
            add(javaExec.absolutePath)
            addAll(jvmArgs.get())
            addAll(listOf("-cp", runtimeClasspath, mainClass.get()))
        }

        ProcessBuilder(cmd)
            .directory(workingDir.get().asFile)
            .inheritIO()
            .start()
    }
}

dependencies {
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.jna)
    implementation(libs.jnativehook)
    implementation(libs.composeIcons.feather)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    implementation(projects.shared.features.clipboard.data)
    implementation(projects.shared.features.clipboard.di)
    implementation(projects.shared.features.syncing.di)
    implementation(projects.shared.features.setting.di)
    implementation(projects.shared.features.notifications.di)
    implementation(projects.shared.features.notifications.domain)
    implementation(projects.shared.features.notifications.presentation)

    implementation(projects.shared.core.ui)

    implementation(libs.tulskiy.jkeymaster)
    implementation(libs.composenativetray)

    implementation(libs.dd.plist)
}

compose.desktop {
    application {
        mainClass = desktopMainClass

        jvmArgs += desktopJvmArgs
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            appResourcesRootDir.set(layout.buildDirectory.dir("generated/macos-notification-bridge"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CrossSync"
            packageVersion = appVersion

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

tasks.matching { it.name == "createDistributable" || it.name == "prepareAppResources" }.configureEach {
    dependsOn(compileMacNotificationBridge)
}

// Gradle держит build-lock на проект пока выполняется `:desktopApp:run` (окно открыто),
// из-за чего Android Studio/CLI не могут запустить второй Gradle build.
// Этот таск запускает приложение "detached" и сразу завершает Gradle build.
tasks.register<RunDetachedTask>("runDetached") {
    group = "application"
    description = "Runs the desktop app in a separate process without blocking Gradle."

    dependsOn(tasks.named("classes"), tasks.named("prepareAppResources"))
    classpath.from(configurations.getByName("runtimeClasspath"))
    mainClass.set(desktopMainClass)
    jvmArgs.set(
        desktopJvmArgs +
            "-Dcompose.application.resources.dir=${layout.buildDirectory.dir("compose/tmp/prepareAppResources").get().asFile.absolutePath}",
    )
    workingDir.set(layout.projectDirectory)
}
