import kotlinx.html.link
import kotlinx.html.meta

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kobweb.application)
}

group = "com.cross.sync.landing"
version = "1.0.0"

kobweb {
    app {
        index {
            description.set("CrossSync synchronizes clipboard history between Android and Desktop over a local network without cloud services.")
            faviconPath.set("/assets/logo.svg")
            head.add {
                meta {
                    attributes["name"] = "robots"
                    attributes["content"] = "index, follow, max-image-preview:large"
                }
                meta {
                    attributes["name"] = "author"
                    attributes["content"] = "Maxim Dvinov"
                }
                meta {
                    attributes["name"] = "keywords"
                    attributes["content"] =
                        "CrossSync, clipboard sync, Android clipboard, desktop clipboard, Kotlin Multiplatform, local network sync"
                }
                meta {
                    attributes["property"] = "og:type"
                    attributes["content"] = "website"
                }
                meta {
                    attributes["property"] = "og:site_name"
                    attributes["content"] = "CrossSync"
                }
                meta {
                    attributes["property"] = "og:image"
                    attributes["content"] = "https://maximdvinov.github.io/CrossSync/assets/logo.svg"
                }
                meta {
                    attributes["name"] = "twitter:card"
                    attributes["content"] = "summary"
                }
                link {
                    attributes["rel"] = "canonical"
                    attributes["href"] = "https://maximdvinov.github.io/CrossSync/"
                }
                link {
                    attributes["rel"] = "alternate"
                    attributes["hreflang"] = "en"
                    attributes["href"] = "https://maximdvinov.github.io/CrossSync/"
                }
                link {
                    attributes["rel"] = "alternate"
                    attributes["hreflang"] = "ru"
                    attributes["href"] = "https://maximdvinov.github.io/CrossSync/ru/"
                }
                link {
                    attributes["rel"] = "alternate"
                    attributes["hreflang"] = "x-default"
                    attributes["href"] = "https://maximdvinov.github.io/CrossSync/"
                }
            }
        }
    }
}

dependencies {
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.kobweb.core)
            implementation(libs.compose.html)
            implementation(libs.kobweb.silk)
        }
    }
}

// CLI helpers for this landing page module.
tasks.register("landingDev") {
    group = "landing"
    description = "Run landing page in development mode with live reload."
    dependsOn("kobwebStart")
}

tasks.register("landingBuildPages") {
    group = "landing"
    description = "Build static export of the landing page for GitHub Pages."
    dependsOn("kobwebExport")
}
