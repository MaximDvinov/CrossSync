#!/bin/bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
configuration=${CONFIGURATION:-Release}
CONFIGURATION="$configuration" "$root/macosApp/scripts/build-framework.sh"
xcodebuild -quiet -destination "platform=macOS,arch=arm64" -project "$root/macosApp/CrossSync.xcodeproj" -scheme CrossSync -configuration "$configuration" -derivedDataPath "$root/macosApp/build/Xcode" CROSSSYNC_SKIP_KOTLIN_BUILD=YES CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=NO build
app="$root/macosApp/build/Xcode/Build/Products/$configuration/CrossSync Native.app"
codesign --force --sign - --options runtime "$app"
codesign --verify --strict "$app"
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT
cp -R "$app" "$staging/"
ln -s /Applications "$staging/Applications"
hdiutil create -volname 'CrossSync Native' -srcfolder "$staging" -ov -format UDZO "$root/macosApp/build/CrossSync-Native-macOS.dmg"
