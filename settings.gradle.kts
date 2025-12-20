rootProject.name = "CrossSync"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

include(":shared:core:ui")

include(":shared:features:clipboard:domain")
include(":shared:features:clipboard:data")
include(":shared:features:clipboard:di")
include(":shared:features:clipboard:db")
include(":shared:features:clipboard:presentation")

include(":shared:features:syncing:network")
include(":shared:features:syncing:domain")
include(":shared:features:syncing:data")
include(":shared:features:syncing:di")
include(":shared:features:syncing:presentation")

include(":shared:features:setting:presentation")
include(":shared:features:setting:domain")
include(":shared:features:setting:di")

include(":shared:features:home")


include(":androidApp")
include(":desktopApp")

