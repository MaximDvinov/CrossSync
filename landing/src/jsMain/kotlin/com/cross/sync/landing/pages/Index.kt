package com.cross.sync.landing.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.navigation.BasePath
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Footer as FooterTag
import org.jetbrains.compose.web.dom.Header as HeaderTag
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main as MainTag
import org.jetbrains.compose.web.dom.Nav as NavTag
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section as SectionTag
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private const val PROJECT_URL = "https://github.com/MaximDvinov/CrossSync"
private const val AUTHOR_URL = "https://github.com/MaximDvinov"

data class LandingCopy(
    val lang: String,
    val otherLangLabel: String,
    val otherLangUrl: String,
    val otherLangCode: String,
    val logoUrl: String,
    val title: String,
    val description: String,
    val eyebrow: String,
    val lead: String,
    val primaryAction: String,
    val secondaryAction: String,
    val cards: List<Pair<String, String>>,
    val workflowTitle: String,
    val workflowText: String,
    val steps: List<String>,
    val techTitle: String,
    val statusTitle: String,
    val roadmapTitle: String,
    val footerTitle: String,
    val footerText: String,
)

val EnglishCopy = LandingCopy(
    lang = "en",
    otherLangLabel = "RU",
    otherLangUrl = "/ru/",
    otherLangCode = "ru",
    logoUrl = "/assets/logo.svg",
    title = "CrossSync",
    description = "Local clipboard and notification synchronization between Android and Desktop without cloud services.",
    eyebrow = "Kotlin Multiplatform clipboard and notification sync",
    lead = "CrossSync keeps clipboard history, Android notifications, pairing, and local network transfer in one focused app.",
    primaryAction = "Open GitHub",
    secondaryAction = "Author profile",
    cards = listOf(
        "Clipboard history" to "Keep copied text, links, and notes available after the original source is gone.",
        "Categories" to "Organize copied items by context and quickly return to what matters.",
        "QR pairing" to "Connect Android to the desktop server with a local QR pairing flow.",
        "LAN first" to "Move data between devices over your network instead of a cloud clipboard.",
        "Notification sync" to "See Android notifications on Desktop with grouped history, media previews, and supported actions or replies."
    ),
    workflowTitle = "Keep clipboard and notifications in sync",
    workflowText = "The desktop app watches the local clipboard and receives Android notifications over LAN, so copied content and incoming messages stay available where you need them.",
    steps = listOf(
        "Desktop stores clipboard history locally.",
        "Android pairs with the desktop server over LAN.",
        "Selected clipboard items can be sent or reused across devices.",
        "Android notifications appear on Desktop with configurable previews and supported actions."
    ),
    techTitle = "Tech stack",
    statusTitle = "Current state",
    roadmapTitle = "Next",
    footerTitle = "Open-source and evolving",
    footerText = "Explore the repository to see modules, architecture, and current development progress."
)

val RussianCopy = LandingCopy(
    lang = "ru",
    otherLangLabel = "EN",
    otherLangUrl = "/",
    otherLangCode = "en",
    logoUrl = "/assets/logo.svg",
    title = "CrossSync",
    description = "Локальная синхронизация буфера обмена и уведомлений между Android и Desktop без облачных сервисов.",
    eyebrow = "Kotlin Multiplatform синхронизация буфера и уведомлений",
    lead = "CrossSync объединяет историю буфера, Android-уведомления, pairing и передачу по локальной сети в одном приложении.",
    primaryAction = "Открыть GitHub",
    secondaryAction = "Профиль автора",
    cards = listOf(
        "История буфера" to "Текст, ссылки и заметки остаются доступны после копирования.",
        "Категории" to "Сохраняйте элементы по контекстам и быстро возвращайтесь к нужному.",
        "QR pairing" to "Android подключается к desktop-серверу через локальный QR flow.",
        "LAN first" to "Данные передаются между устройствами в вашей сети, без облачного буфера.",
        "Синхронизация уведомлений" to "Уведомления Android появляются на Desktop: с группировкой, медиа-превью, действиями и ответами, если их поддерживает приложение."
    ),
    workflowTitle = "Синхронизируйте буфер и уведомления",
    workflowText = "Desktop-приложение отслеживает локальный буфер и получает уведомления Android по LAN, поэтому скопированный контент и входящие сообщения доступны там, где нужны.",
    steps = listOf(
        "Desktop сохраняет локальную историю буфера.",
        "Android подключается к серверу в локальной сети.",
        "Выбранные элементы можно отправлять и переиспользовать между устройствами.",
        "Уведомления Android появляются на Desktop с настраиваемым превью и поддерживаемыми действиями."
    ),
    techTitle = "Технологии",
    statusTitle = "Сейчас",
    roadmapTitle = "Дальше",
    footerTitle = "Открытый исходный код",
    footerText = "В репозитории можно посмотреть модули, архитектуру и текущий прогресс разработки."
)

@Page
@Composable
fun HomePage() {
    LandingPage(EnglishCopy)
}

@Composable
fun LandingPage(copy: LandingCopy) {
    PageMetadata(copy)

    Column(Modifier.css("min-height:100vh;width:100%;align-items:stretch;background:#F0F9FF;color:#00253C;font-family:'Segoe UI',Arial,sans-serif;")) {
        SiteHeader(copy)
        MainTag(Modifier.css("width:100%;max-width:1120px;margin:0 auto;padding:32px 20px 56px;gap:34px;box-sizing:border-box;display:flex;flex-direction:column;align-items:stretch;").toAttrs()) {
            Hero(copy)
            FeatureGrid(copy)
            Workflow(copy)
            Details(copy)
            Footer(copy)
        }
    }
}

@Composable
private fun PageMetadata(copy: LandingCopy) {
    LaunchedEffect(copy.lang) {
        val canonical = if (copy.lang == "ru") "https://maximdvinov.github.io/CrossSync/ru/" else "https://maximdvinov.github.io/CrossSync/"
        document.documentElement?.setAttribute("lang", copy.lang)
        document.title = if (copy.lang == "ru") "${copy.title} | Локальная синхронизация буфера и уведомлений" else "${copy.title} | Local clipboard and notification sync"
        setMeta("description", copy.description)
        setMeta("robots", "index, follow, max-image-preview:large")
        setMeta("og:title", copy.title, property = true)
        setMeta("og:description", copy.description, property = true)
        setMeta("og:url", canonical, property = true)
        setMeta("og:locale", if (copy.lang == "ru") "ru_RU" else "en_US", property = true)
        setMeta("twitter:title", copy.title)
        setMeta("twitter:description", copy.description)
        setLink("canonical", canonical)
        setJsonLd(copy)
    }
}

private fun setMeta(name: String, content: String, property: Boolean = false) {
    val key = if (property) "property" else "name"
    val selector = "meta[$key='$name']"
    val head = document.head ?: return
    val element = head.querySelector(selector) ?: document.createElement("meta").also {
        it.setAttribute(key, name)
        head.appendChild(it)
    }
    element.setAttribute("content", content)
}

private fun setLink(rel: String, href: String) {
    val head = document.head ?: return
    val element = head.querySelector("link[rel='$rel']") ?: document.createElement("link").also {
        it.setAttribute("rel", rel)
        head.appendChild(it)
    }
    element.setAttribute("href", href)
}

private fun setJsonLd(copy: LandingCopy) {
    val head = document.head ?: return
    val element = head.querySelector("script#crosssync-structured-data") ?: document.createElement("script").also {
        it.setAttribute("id", "crosssync-structured-data")
        it.setAttribute("type", "application/ld+json")
        head.appendChild(it)
    }
    element.textContent = copy.schemaJson()
}

@Composable
private fun SiteHeader(copy: LandingCopy) {
    HeaderTag(Modifier.css("width:100%;max-width:1120px;margin:0 auto;padding:18px 20px 0;box-sizing:border-box;display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;").toAttrs()) {
        Row(Modifier.css("align-items:center;gap:12px;")) {
            Logo(44, copy.logoUrl)
            Column {
                P(Modifier.css("margin:0;font-size:18px;font-weight:700;color:#00253C;").toAttrs()) { Text("CrossSync") }
                P(Modifier.css("margin:2px 0 0;font-size:13px;color:#7989A2;").toAttrs()) { Text("Android + Desktop") }
            }
        }
        NavTag(Modifier.css("display:flex;align-items:center;gap:10px;flex-wrap:wrap;").toAttrs {
            attr("aria-label", if (copy.lang == "ru") "Основная навигация" else "Primary navigation")
        }) {
            NavLink(copy.otherLangLabel, copy.otherLangUrl, copy.otherLangCode)
            ButtonLink(copy.primaryAction, PROJECT_URL, filled = true)
        }
    }
}

@Composable
private fun Hero(copy: LandingCopy) {
    SectionTag(Modifier.css("width:100%;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr));gap:40px;align-items:center;").toAttrs {
        attr("aria-labelledby", "hero-title")
    }) {
        Column(Modifier.css("width:100%;min-width:0;gap:16px;align-items:flex-start;")) {
            Pill(copy.eyebrow)
            H1(Modifier.css("margin:0;font-size:44px;line-height:1.08;font-weight:800;color:#00253C;").toAttrs {
                attr("id", "hero-title")
            }) { Text(copy.title) }
            P(Modifier.css("margin:0;font-size:22px;line-height:1.45;color:#00253C;max-width:680px;").toAttrs()) { Text(copy.description) }
            P(Modifier.css("margin:0;font-size:16px;line-height:1.7;color:#7989A2;max-width:680px;").toAttrs()) { Text(copy.lead) }
            Row(Modifier.css("gap:10px;flex-wrap:wrap;margin-top:8px;")) {
                ButtonLink(copy.primaryAction, PROJECT_URL, filled = true)
                ButtonLink(copy.secondaryAction, AUTHOR_URL, filled = false)
            }
        }
        AppPreview(copy)
    }
}

@Composable
private fun AppPreview(copy: LandingCopy) {
    Column(Modifier.css("width:100%;min-width:0;justify-self:stretch;align-items:stretch;background:#FEFFFF;border:1px solid rgba(95,146,221,.22);border-radius:24px;box-shadow:0 16px 36px rgba(0,37,60,.10);overflow:hidden;box-sizing:border-box;")) {
        Row(Modifier.css("width:100%;height:58px;background:#DCECFF;align-items:center;justify-content:space-between;padding:0 18px;box-sizing:border-box;")) {
            Row(Modifier.css("align-items:center;gap:10px;color:#02609B;font-weight:700;")) {
                Logo(28, copy.logoUrl)
                Span { Text("Clipboard") }
            }
            Span(Modifier.css("font-size:13px;color:#7989A2;").toAttrs()) { Text("Connected") }
        }
        Column(Modifier.css("width:100%;padding:18px;gap:12px;align-items:stretch;box-sizing:border-box;")) {
            Row(Modifier.css("width:100%;gap:8px;flex-wrap:wrap;")) {
                Tag("All", selected = true)
                Tag("Links")
                Tag("Notes")
            }
            ClipboardItem(PROJECT_URL, "Link")
            ClipboardItem(copy.workflowTitle, "Text")
            ClipboardItem(copy.lead, "Note")
        }
    }
}

@Composable
private fun FeatureGrid(copy: LandingCopy) {
    SectionTag(Modifier.css("width:100%;gap:18px;align-items:stretch;display:flex;flex-direction:column;").toAttrs {
        attr("aria-labelledby", "features-title")
    }) {
        H2(Modifier.css("margin:0;font-size:30px;line-height:1.2;color:#00253C;").toAttrs {
            attr("id", "features-title")
        }) {
            Text(if (copy.lang == "ru") "Что умеет приложение" else "What the app does")
        }
        Div(Modifier.css("width:100%;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:14px;").toAttrs()) {
            copy.cards.forEach { (title, text) -> FeatureCard(title, text) }
        }
    }
}

@Composable
private fun Workflow(copy: LandingCopy) {
    SectionTag(Modifier.css("width:100%;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr));gap:18px;align-items:stretch;").toAttrs {
        attr("aria-labelledby", "workflow-title")
    }) {
        Column(Modifier.css("width:100%;align-items:stretch;background:#FEFFFF;border:1px solid rgba(95,146,221,.18);border-radius:24px;padding:22px;box-shadow:0 16px 36px rgba(0,37,60,.08);gap:14px;box-sizing:border-box;")) {
            H2(Modifier.css("margin:0;font-size:28px;line-height:1.2;color:#00253C;").toAttrs {
                attr("id", "workflow-title")
            }) { Text(copy.workflowTitle) }
            P(Modifier.css("margin:0;color:#7989A2;font-size:16px;line-height:1.65;").toAttrs()) { Text(copy.workflowText) }
            copy.steps.forEachIndexed { index, step -> Step(index + 1, step) }
        }
        Column(Modifier.css("width:100%;align-items:stretch;background:#00253C;border-radius:24px;padding:24px;color:#F7FCFF;gap:18px;justify-content:space-between;box-sizing:border-box;")) {
            H3(Modifier.css("margin:0;font-size:24px;line-height:1.25;color:#F7FCFF;").toAttrs()) { Text("QR Pairing") }
            Div(Modifier.css("width:172px;height:172px;border-radius:18px;background:#F7FCFF;margin:10px auto;display:grid;grid-template-columns:repeat(5,1fr);gap:8px;padding:16px;box-sizing:border-box;").toAttrs()) {
                repeat(25) { index ->
                    val active = index in listOf(0, 1, 3, 4, 5, 8, 10, 12, 14, 16, 18, 20, 21, 23, 24)
                    Div(Modifier.css("border-radius:4px;background:${if (active) "#00253C" else "#DCECFF"};").toAttrs()) {}
                }
            }
            P(Modifier.css("margin:0;color:#DCECFF;font-size:14px;line-height:1.55;").toAttrs()) {
                Text(if (copy.lang == "ru") "Локальное подключение без облачного буфера." else "Local pairing without a cloud clipboard.")
            }
        }
    }
}

@Composable
private fun Details(copy: LandingCopy) {
    SectionTag(Modifier.css("width:100%;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),1fr));gap:14px;").toAttrs {
        attr("aria-label", if (copy.lang == "ru") "Технические детали проекта" else "Project technical details")
    }) {
        InfoBlock(copy.techTitle, "Kotlin Multiplatform, Compose Multiplatform, Ktor WebSocket, Koin, Room, SQLite.")
        InfoBlock(copy.statusTitle, if (copy.lang == "ru") "История, категории, Android-клиент, desktop-клиент, QR pairing, синхронизация уведомлений и текстовая синхронизация." else "History, categories, Android client, desktop client, QR pairing, notification sync, and text synchronization.")
        InfoBlock(copy.roadmapTitle, if (copy.lang == "ru") "Синхронизация изображений и файлов, расширение desktop-платформ, публичные релизы." else "Image and file sync, broader desktop support, and public release artifacts.")
    }
}

@Composable
private fun Footer(copy: LandingCopy) {
    FooterTag(Modifier.css("width:100%;background:#DCECFF;border-radius:24px;padding:24px;display:flex;align-items:center;justify-content:space-between;gap:18px;flex-wrap:wrap;box-sizing:border-box;").toAttrs()) {
        Column(Modifier.css("gap:8px;max-width:680px;")) {
            H2(Modifier.css("margin:0;color:#00253C;font-size:26px;").toAttrs()) { Text(copy.footerTitle) }
            P(Modifier.css("margin:0;color:#02609B;font-size:15px;line-height:1.55;").toAttrs()) { Text(copy.footerText) }
        }
        ButtonLink(copy.primaryAction, PROJECT_URL, filled = true)
    }
}

@Composable
private fun FeatureCard(title: String, text: String) {
    Column(Modifier.css("width:100%;height:100%;padding:18px;border-radius:16px;border:1px solid rgba(95,146,221,.20);background:#FEFFFF;box-shadow:0 10px 24px rgba(0,37,60,.06);gap:10px;box-sizing:border-box;")) {
        H3(Modifier.css("margin:0;color:#00253C;font-size:19px;font-weight:700;").toAttrs()) { Text(title) }
        P(Modifier.css("margin:0;color:#7989A2;font-size:15px;line-height:1.55;").toAttrs()) { Text(text) }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(Modifier.css("width:100%;align-items:flex-start;gap:12px;")) {
        Div(Modifier.css("width:28px;height:28px;border-radius:10px;background:#DCECFF;color:#02609B;display:grid;place-items:center;font-weight:700;flex:0 0 auto;").toAttrs()) {
            Text(number.toString())
        }
        P(Modifier.css("margin:3px 0 0;color:#00253C;font-size:15px;line-height:1.55;").toAttrs()) { Text(text) }
    }
}

@Composable
private fun ClipboardItem(value: String, label: String) {
    Column(Modifier.css("width:100%;border:1px solid rgba(95,146,221,.18);border-radius:16px;background:#F7FCFF;padding:14px;gap:8px;box-sizing:border-box;")) {
        Span(Modifier.css("color:#02609B;font-size:13px;font-weight:700;").toAttrs()) { Text(label) }
        P(Modifier.css("margin:0;color:#00253C;font-size:15px;line-height:1.45;word-break:break-word;").toAttrs()) { Text(value) }
    }
}

@Composable
private fun InfoBlock(title: String, text: String) {
    Column(Modifier.css("width:100%;height:100%;border-left:4px solid #5F92DD;background:#FEFFFF;border-radius:16px;padding:18px;border-top:1px solid rgba(95,146,221,.14);border-right:1px solid rgba(95,146,221,.14);border-bottom:1px solid rgba(95,146,221,.14);gap:10px;box-sizing:border-box;")) {
        H3(Modifier.css("margin:0;color:#00253C;font-size:18px;font-weight:700;").toAttrs()) { Text(title) }
        P(Modifier.css("margin:0;color:#7989A2;font-size:15px;line-height:1.6;").toAttrs()) { Text(text) }
    }
}

@Composable
private fun Logo(size: Int, url: String) {
    Img(src = BasePath.prependTo(url), attrs = {
        alt("CrossSync logo")
        attr("width", size.toString())
        attr("height", size.toString())
    })
}

@Composable
private fun Pill(text: String) {
    Span(Modifier.css("display:inline-flex;width:max-content;border-radius:12px;background:#DCECFF;color:#02609B;padding:8px 12px;font-size:13px;font-weight:700;").toAttrs()) {
        Text(text)
    }
}

@Composable
private fun Tag(text: String, selected: Boolean = false) {
    val style = if (selected) {
        "border-radius:12px;background:#5F92DD;color:#F7FCFF;padding:8px 12px;font-size:13px;font-weight:700;"
    } else {
        "border-radius:12px;background:#DCECFF;color:#02609B;padding:8px 12px;font-size:13px;font-weight:700;"
    }
    Span(Modifier.css(style).toAttrs()) { Text(text) }
}

@Composable
private fun ButtonLink(label: String, url: String, filled: Boolean) {
    val style = if (filled) {
        "display:inline-flex;align-items:center;justify-content:center;text-decoration:none;padding:12px 16px;border-radius:12px;border:1px solid #5F92DD;background:#5F92DD;color:#F7FCFF;font-weight:700;font-size:14px;"
    } else {
        "display:inline-flex;align-items:center;justify-content:center;text-decoration:none;padding:12px 16px;border-radius:12px;border:1px solid rgba(121,137,162,.30);background:#FEFFFF;color:#00253C;font-weight:700;font-size:14px;"
    }
    A(attrs = {
        href(url)
        target(ATarget.Blank)
        attr("rel", "noopener noreferrer")
        attr("style", style)
    }) { Text(label) }
}

@Composable
private fun NavLink(label: String, url: String, lang: String) {
    A(attrs = {
        href(BasePath.prependTo(url))
        attr("hreflang", lang)
        attr("aria-label", if (lang == "ru") "Русская версия" else "English version")
        attr("style", "display:inline-flex;align-items:center;justify-content:center;min-width:42px;height:42px;border-radius:12px;background:#FEFFFF;color:#02609B;text-decoration:none;font-weight:800;border:1px solid rgba(95,146,221,.20);")
    }) { Text(label) }
}

private fun Modifier.css(value: String): Modifier = attrsModifier {
    attr("style", value)
}

private fun LandingCopy.schemaJson(): String {
    val canonical = if (lang == "ru") "https://maximdvinov.github.io/CrossSync/ru/" else "https://maximdvinov.github.io/CrossSync/"
    val appName = title.json()
    val appDescription = description.json()
    val language = lang.json()

    return """
        {
          "@context": "https://schema.org",
          "@graph": [
            {
              "@type": "WebSite",
              "@id": "https://maximdvinov.github.io/CrossSync/#website",
              "url": "https://maximdvinov.github.io/CrossSync/",
              "name": $appName,
              "inLanguage": $language
            },
            {
              "@type": "SoftwareApplication",
              "@id": "$canonical#software",
              "name": $appName,
              "description": $appDescription,
              "url": "$canonical",
              "applicationCategory": "UtilitiesApplication",
              "operatingSystem": "Android, Desktop",
              "softwareRequirements": "Local network connection",
              "codeRepository": "$PROJECT_URL",
              "isAccessibleForFree": true,
              "inLanguage": $language,
              "author": {
                "@type": "Person",
                "name": "Maxim Dvinov",
                "url": "$AUTHOR_URL"
              },
              "publisher": {
                "@type": "Person",
                "name": "Maxim Dvinov",
                "url": "$AUTHOR_URL"
              }
            }
          ]
        }
    """.trimIndent()
}

private fun String.json(): String = buildString {
    append('"')
    this@json.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}
