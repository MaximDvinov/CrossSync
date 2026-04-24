#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_CMD=(./gradlew :landing:kobwebExport -PkobwebEnv=DEV -PkobwebRunLayout=FULLSTACK -PkobwebBuildTarget=RELEASE -PkobwebExportLayout=STATIC -PkobwebReuseServer=false)

usage() {
  echo "Usage: ./scripts/landing-pages.sh <build|publish>"
}

build_site() {
  "${GRADLE_CMD[@]}"
  echo "Landing static export is ready in: $ROOT_DIR/landing/.kobweb/site"
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
