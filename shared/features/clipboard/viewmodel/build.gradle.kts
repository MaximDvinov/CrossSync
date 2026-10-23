plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}
kotlin {
    android {
        namespace = "com.cross.sync.clipboard.viewmodel"
        compileSdk = 37
        minSdk = 24
    }
    jvm()
    macosArm64()
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodelCore)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(projects.shared.features.clipboard.domain)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
        jvmMain.dependencies { implementation(libs.kotlinx.coroutines.swing) }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
