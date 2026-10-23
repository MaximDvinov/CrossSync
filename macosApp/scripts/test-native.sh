#!/bin/bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
configuration=${CROSSSYNC_TEST_CONFIGURATION:-Debug}
variant=$(printf '%s' "$configuration" | tr '[:upper:]' '[:lower:]')
framework="$root/shared/macos/build/bin/macosArm64/${variant}Framework"
test -d "$framework/CrossSyncShared.framework"
mkdir -p "$root/macosApp/build/tests"
xcrun swiftc -swift-version 6 -target arm64-apple-macos26.0 -F "$framework" -framework CrossSyncShared -framework CoreFoundation -framework Security -lc++ -lsqlite3 \
    "$root/macosApp/Sources/PlatformServices/CryptoKitOperations.swift" \
    "$root/macosApp/Sources/PlatformServices/MacDesktopServices.swift" \
    "$root/macosApp/Tests/NativeIntegrationTests.swift" -o "$root/macosApp/build/tests/NativeIntegrationTests"
"$root/macosApp/build/tests/NativeIntegrationTests"

xcrun swiftc -swift-version 6 -target arm64-apple-macos26.0 -F "$framework" -framework CrossSyncShared -framework CoreFoundation -framework Security -lc++ -lsqlite3 \
    "$root/macosApp/Sources/Clipboard/ClipboardPresentation.swift" \
    "$root/macosApp/Tests/ClipboardPresentationTests.swift" -o "$root/macosApp/build/tests/ClipboardPresentationTests"
"$root/macosApp/build/tests/ClipboardPresentationTests"
