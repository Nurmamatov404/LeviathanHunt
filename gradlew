#!/bin/sh
DIR="$( cd "$( dirname "$0" )" && pwd )"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
elif [ -f "$DIR/gradlew" ]; then
  # In CI, setup-gradle action makes gradle available
  echo "Gradle not found in PATH. Fallback to wrapper."
  # Read version from properties
  PROPS="$DIR/gradle/wrapper/gradle-wrapper.properties"
  if [ -f "$PROPS" ]; then
    VER=$(grep "^distributionUrl" "$PROPS" | sed 's/.*gradle-\([0-9.]*\)-bin.zip.*/\1/')
    CACHE_DIR="$HOME/.gradle/wrapper/dists/gradle-$VER-bin"
    if [ -f "$CACHE_DIR/bin/gradle" ]; then
      exec "$CACHE_DIR/bin/gradle" "$@"
    fi
  fi
  echo "Error: Gradle not installed. Install Gradle $VER or use gradle/actions/setup-gradle in CI."
  exit 1
fi
