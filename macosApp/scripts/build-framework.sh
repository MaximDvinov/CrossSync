#!/bin/bash
set -euo pipefail
if [[ "${CROSSSYNC_SKIP_KOTLIN_BUILD:-NO}" == YES ]]; then exit 0; fi
root=$(cd "$(dirname "$0")/../.." && pwd)
configuration=${CONFIGURATION:-Debug}
variant=$(printf '%s' "$configuration" | tr '[:upper:]' '[:lower:]')
cd "$root"
tasks=(":shared:macos:link${configuration}FrameworkMacosArm64")
if [[ -n "${CROSSSYNC_GRADLE_RUNNER:-}" && -n "${CROSSSYNC_GRADLE_WORKFLOW:-}" ]]; then
    python3 "$CROSSSYNC_GRADLE_RUNNER" run --workflow "$CROSSSYNC_GRADLE_WORKFLOW" --scope targeted --question "Does the Apple Silicon Kotlin framework link?" -- ./gradlew "${tasks[@]}"
else
    ./gradlew "${tasks[@]}"
fi
output="$root/macosApp/build/frameworks/$configuration"
mkdir -p "$output"
rm -rf "$output/CrossSyncShared.framework"
cp -R "$root/shared/macos/build/bin/macosArm64/${variant}Framework/CrossSyncShared.framework" "$output/"
