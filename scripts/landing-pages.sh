#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

KOBWEB_REUSE_SERVER=true
if [[ "${CI:-}" == "true" ]]; then
  KOBWEB_REUSE_SERVER=false
fi
APP_VERSION="${APP_VERSION:-1.5.1}"

GRADLE_CMD=(./gradlew :landing:kobwebExport -PappVersion="$APP_VERSION" -PkobwebEnv=DEV -PkobwebRunLayout=FULLSTACK -PkobwebBuildTarget=RELEASE -PkobwebExportLayout=STATIC -PkobwebReuseServer="$KOBWEB_REUSE_SERVER")

usage() {
  echo "Usage: ./scripts/landing-pages.sh <build|publish>"
}

build_site() {
  "${GRADLE_CMD[@]}"
  fix_github_pages_paths
  echo "Landing static export is ready in: $ROOT_DIR/landing/.kobweb/site"
}

fix_github_pages_paths() {
  local site_dir="$ROOT_DIR/landing/.kobweb/site"

  # Kobweb emits the app bundle as an absolute root URL. GitHub Pages serves this
  # repository under /CrossSync, so keep bundle links relative to each page.
  if [[ -f "$site_dir/index.html" ]]; then
    perl -0pi -e 's#src="/landing\.js"#src="landing.js"#g' "$site_dir/index.html"
  fi

  if [[ -f "$site_dir/ru/index.html" ]]; then
    perl -0pi -e 's#src="/landing\.js"#src="../landing.js"#g' "$site_dir/ru/index.html"
  fi
}

publish_site() {
  build_site

  if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI (gh) is required for publish command."
    echo "Install gh or publish by pushing to main to trigger workflow."
    exit 1
  fi

  gh workflow run landing-pages.yml --ref main
  echo "Publication workflow has been triggered."
}

case "${1:-}" in
  build)
    build_site
    ;;
  publish)
    publish_site
    ;;
  *)
    usage
    exit 1
    ;;
esac
