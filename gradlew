#!/usr/bin/env sh
set -eu
VERSION=8.14.6
HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/tessera-gradle/gradle-$VERSION"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  mkdir -p "$(dirname "$HOME_DIR")"
  ARCHIVE="$(dirname "$HOME_DIR")/gradle-$VERSION-bin.zip"
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ARCHIVE"
  unzip -q -o "$ARCHIVE" -d "$(dirname "$HOME_DIR")"
  rm -f "$ARCHIVE"
fi
exec "$HOME_DIR/bin/gradle" "$@"
