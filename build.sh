#!/usr/bin/env bash
# Builds the mod. Needs JDK 25 on PATH (or JAVA_HOME) and, the first time, a `gradle` install
# (only used to generate the wrapper) - or run it once with an existing ./gradlew.
set -e
cd "$(dirname "$0")"
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  command -v gradle >/dev/null || { echo "Install Gradle once (to generate the wrapper), or open the folder in IntelliJ."; exit 1; }
  gradle wrapper --gradle-version 9.6.0
fi
chmod +x gradlew
./gradlew build --stacktrace
mkdir -p dist && cp build/libs/*.jar dist/
echo "Done -> dist/"
