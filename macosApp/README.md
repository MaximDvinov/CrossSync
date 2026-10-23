# CrossSync Native для macOS

Нативное SwiftUI-приложение для macOS 26 и новее, только Apple Silicon (`arm64`). Android и Compose Desktop продолжают использовать те же Kotlin ViewModel, репозитории, DAO и протокол.

## Сборка

Требуются Mac с Apple Silicon, Xcode 26.0+ со Swift 6.2+, JDK 21 и настроенный Android SDK для конфигурации существующих Gradle-модулей. Используйте актуальную совместимую версию Xcode для установленной macOS.

Откройте `macosApp/CrossSync.xcodeproj`, выберите схему `CrossSync` и запустите приложение. Build phase собирает `CrossSyncShared.framework` через Gradle. Для сборки из корня репозитория:

```bash
bash macosApp/scripts/package.sh
```

Результат:

- `macosApp/build/Xcode/Build/Products/Release/CrossSync Native.app`
- `macosApp/build/CrossSync-Native-macOS.dmg`

Пакет подписывается локальной ad-hoc подписью с идентификатором приложения и sealed resources. Сборка не включает Developer ID, notarization и публикацию релиза. Kotlin framework статический; JVM для нативного приложения не требуется. Архитектура Intel не собирается.

## Владение состоянием и данными

`shared/features/{clipboard,setting,notifications}/viewmodel` содержат существующие `ClipboardViewModel`, `DesktopSettingViewModel`, `NotificationViewModel` без Compose UI. Пакеты классов сохранены, Compose presentation подключает новые модули. Выборы нативных Picker передаются прямым сеттерам Kotlin; методы циклического переключения сохранены.

`shared/macos` экспортирует `CrossSyncShared` через Objective-C interop. `MacRuntime` создаёт локальный контейнер Koin и один `DesktopRuntime` на приложение. Монитор clipboard, Room, CIO и фоновые задачи живут независимо от окон. Экранные модели имеют типизированные подписки с `cancel()`, собственный `ViewModelStore` и `close()`. Swift `@MainActor @Observable`-адаптеры только отображают состояние и отправляют команды. При завершении runtime дожидается отмены ViewModel, запросов базы и сервера, затем закрывает Room.

Swift-код разделён на App, Clipboard, Notifications, Settings, QuickAccess и PlatformServices. Структура повторяет Compose Desktop: компактная шапка CrossSync с кнопками уведомлений и настроек, горизонтальные категории и список карточек на всю ширину. Выбранная карточка выделяется акцентной рамкой. Один клик копирует запись; Return копирует выбранную запись. Поиск раскрывается кнопкой или ⌘F. Полный предпросмотр, перемещение и удаление доступны в меню карточки, очистка категории — сверху с подтверждением. Уведомления отображаются лентой карточек с переключателем Active/History; настройки открываются в том же окне с кнопкой возврата.

`ClipboardView`, `NotificationsView`, `NotificationCard`, `SettingsView` и sheets получают только состояние Kotlin ViewModel и `onEvent`. Типизированные `ClipboardEvent`, `NotificationsEvent`, `SettingsEvent` обрабатываются Swift-адаптерами; там же находятся обращения к системным разрешениям. Состояние сопряжения читается непосредственно из `SettingState.pairingState`. Поиск, выделение строки, навигация, черновики и видимость диалогов — локальное состояние интерфейса; настройки и данные берутся из ViewModel без локальных копий.

Настройки собраны в одну прокручиваемую `Form`: General, Clipboard, Quick Access, Notifications, Categories, Excluded Applications и Devices. Тот же экран открывается через системную `Settings` scene (⌘,). Выбор исключённых приложений и сопряжение открываются в sheets. Quick Access размером 440×560 pt повторяет структуру главного окна с меньшими отступами; поиск по умолчанию скрыт, снизу остаётся тонкая подсказка клавиш. Стрелки выбирают запись, Return или двойной клик копируют и запускают вставку в предыдущее приложение. Один клик только копирует.

Карточки показывают HTML/RTF-форматирование, иконку и название приложения, тип данных и дату со временем. Текст ограничен двумя строками в Quick Access и четырьмя в главном окне; изображения — 64 и 80 pt по высоте соответственно. Полный текст и изображения доступны в sheet предпросмотра. Форматирование кэшируется, нейтральные цвета адаптируются к теме macOS; исходный payload не изменяется при отображении или копировании. Quick Access и всплывающие уведомления используют SwiftUI в `NSPanel`. Закрытие окон сохраняет работу в строке меню; Quit завершает runtime.

Room сохраняет существующую схему и миграции. Нативная версия использует отдельные данные:

- `~/Library/Application Support/CrossSyncNative/clipboard.db`
- `~/Library/Application Support/CrossSyncNative/Images/`
- UserDefaults suite `com.cross.sync.macos.preferences`

Старое хранилище Compose Desktop не импортируется. История файлов хранит исходные пути; копирование недоступного файла показывает ошибку. Изображения сохраняются локально. Через сеть передаются только текст/HTML/RTF и существующие сообщения уведомлений.

CIO сначала использует сохранённый порт, а при первом запуске — `33333`. Если порт занят, ОС выбирает свободный. Приложение сохраняет выбранный порт для следующих запусков и указывает его в QR-коде после готовности сервера. Если сохранённый порт впоследствии занят и меняется, телефон нужно повторно сопрячь через QR-код. Другая версия CrossSync автоматически не завершается. AES-256-GCM CryptoKit сохраняет Base64-формат `nonce(12) + ciphertext + tag(16)`. JWT HS256 сохраняет claims и сроки: access 15 минут, refresh 30 дней. Ключ подписи принадлежит новому хранилищу. Удаление устройства закрывает его WebSocket.

## Проверки

```bash
./gradlew :shared:core:db:macosArm64Test \
  :shared:features:syncing:runtime:jvmTest \
  :shared:features:syncing:runtime:macosArm64Test \
  :shared:features:notifications:viewmodel:jvmTest \
  :shared:features:syncing:network:jvmTest \
  :shared:features:syncing:domain:jvmTest \
  :shared:macos:linkDebugFrameworkMacosArm64
bash macosApp/scripts/test-native.sh
./gradlew :androidApp:assembleDebug :desktopApp:compileKotlin
```

Нативные интеграционные тесты используют отдельные NSPasteboard, UserDefaults, временную базу и порт. Они проверяют CryptoKit/NIST и изменённые ciphertext, локальные текст/HTML/RTF/изображения/файлы, недоступные файлы, категории, исключённые приложения, отмену подписок, несколько окон, выбор свободного порта при конфликте и сохранение порта после перезапуска, сопряжение, JWT/refresh, шифрованный WebSocket, snapshot/reconnect, уведомления и ответы/действия, offline-ошибки, Live Update и восстановление истории без повторных баннеров. `ClipboardPresentationTests` проверяют сохранение жирного/курсивного текста HTML/RTF, подчёркивание, ссылки, компактный размер шрифта, цвета текста и фона, fallback повреждённых данных и иконки приложений. Kotlin-тесты проверяют режимы доставки, действия уведомлений, Room и параллельные подписки. Отдельный workflow `.github/workflows/native-macos.yml` собирает и сохраняет `.app`/`.dmg` как CI artifacts.

Локальная проверка выполнена на macOS 27.0 с Xcode 27; deployment target приложения — macOS 26.0. Интеграционные тесты прошли с Debug и Release framework. Обновлённый интерфейс проверяется в Debug: главное окно, настройки, уведомления, Quick Access через ⌘⇧V, стрелки, поиск, предпросмотр и Escape. При повторной интеграционной проверке один прогон остановился на ожидании отключения WebSocket; повторный прогон прошёл. Проверка на настоящем Android, предоставление/отказ Accessibility и уведомлений, вставка в сторонние приложения, автозапуск после входа, несколько физических мониторов и полный проход VoiceOver требуют проверки на устройствах. Они не подтверждены автоматическими тестами.
