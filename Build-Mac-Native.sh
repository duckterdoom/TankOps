#!/bin/bash
set -euo pipefail
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
[[ "$(uname -s)" == Darwin ]] || { echo 'Run this build on macOS with JDK 21+.' >&2; exit 1; }
if [[ -n "${JAVA_HOME:-}" ]]; then jdk="$JAVA_HOME"; else jdk=$(/usr/libexec/java_home -v 21+); fi
build_root=$(mktemp -d "$project_root/target-mac.XXXXXX")
mkdir -p "$build_root/classes"
sources="$build_root/sources.txt"
find "$project_root/tankops-common/src/main/java" "$project_root/tankops-client/src/main/java" \
    -name '*.java' -type f | LC_ALL=C sort | while IFS= read -r source; do printf '"%s"\n' "$source"; done > "$sources"
"$jdk/bin/javac" --release 21 -encoding UTF-8 -d "$build_root/classes" "@$sources"
for module in tankops-common tankops-client; do
    cp -R "$project_root/$module/src/main/resources/." "$build_root/classes/"
done
"$jdk/bin/jar" --create --file "$build_root/TankOps.jar" --main-class tankops.client.Main -C "$build_root/classes" .
JAVA_HOME="$jdk" TANKOPS_JAR="$build_root/TankOps.jar" TANKOPS_OUTPUT_DIR="$build_root" \
    bash "$project_root/packaging/macos/Build-TankOps-App.command"
app_zip=$(find "$build_root" -name 'TankOps-App.zip' -type f -print)
arch=$("$jdk/bin/java" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/os.arch =/ {print $2}')
destination="$project_root/TankOps-Mac-$arch.zip"
cp "$app_zip" "$destination"
echo "Ready: $destination (bundled Java; user does not need to install Java)."
