#!/bin/sh
DIR="$( cd "$( dirname "$0" )" && pwd )"

# Always use the Gradle version from gradle-wrapper.properties
# Never use system-installed Gradle (may have incompatible version)
PROPS="$DIR/gradle/wrapper/gradle-wrapper.properties"
if [ -f "$PROPS" ]; then
  VER=$(grep "^distributionUrl" "$PROPS" | sed 's/.*gradle-\([0-9.]*\)-bin.zip.*/\1/')
  CACHE_DIR="$HOME/.gradle/wrapper/dists/gradle-$VER-bin"
  if [ -f "$CACHE_DIR/bin/gradle" ]; then
    exec "$CACHE_DIR/bin/gradle" "$@"
  fi
fi

JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$JAR" ]; then
  exec java -jar "$JAR" "$@"
fi

echo "Error: Gradle $VER not found. Run 'gradle wrapper --gradle-version $VER' to regenerate."
exit 1
