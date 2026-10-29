#!/bin/bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
[[ "$(uname -s)" == Linux ]] || { echo 'Build the Ubuntu runtime on Linux with JDK 21+.' >&2; exit 1; }
if [[ -n "${JAVA_HOME:-}" ]]; then jdk="$JAVA_HOME"; else jdk=$(dirname -- "$(dirname -- "$(readlink -f "$(command -v javac)")")"); fi
mkdir -p "$project_root/target"
build_root=$(mktemp -d "$project_root/target/server-native.XXXXXX")
bundle="$build_root/TankOps-Ubuntu"
mkdir -p "$build_root/classes" "$bundle"
find "$project_root/tankops-common/src/main/java" "$project_root/tankops-server/src/main/java" \
    -name '*.java' -type f | LC_ALL=C sort | while IFS= read -r source; do printf '"%s"\n' "$source"; done > "$build_root/sources.txt"
"$jdk/bin/javac" --release 21 -encoding UTF-8 -d "$build_root/classes" "@$build_root/sources.txt"
for module in tankops-common tankops-server; do cp -R "$project_root/$module/src/main/resources/." "$build_root/classes/"; done
"$jdk/bin/jar" --create --file "$bundle/tankops-server.jar" --main-class tankops.server.net.GameServer -C "$build_root/classes" .
modules=$("$jdk/bin/jdeps" --print-module-deps "$bundle/tankops-server.jar")
"$jdk/bin/jlink" --module-path "$jdk/jmods" --add-modules "$modules" \
    --strip-debug --no-header-files --no-man-pages --output "$bundle/runtime"
cp "$project_root/deploy/ubuntu/"* "$bundle/"
sed -e 's|](README.md)|](HUONG-DAN.md)|g' -e 's|](docs/protocol.md)|](protocol.md)|g' "$project_root/SYSTEM_ANALYSIS.md" > "$bundle/HUONG-DAN.md"
sed 's|](../SYSTEM_ANALYSIS.md)|](HUONG-DAN.md)|g' "$project_root/docs/protocol.md" > "$bundle/protocol.md"
env -u JAVA_HOME -u JAVA_TOOL_OPTIONS -u _JAVA_OPTIONS -u JDK_JAVA_OPTIONS \
    PATH=/usr/bin:/bin "$bundle/runtime/bin/java" "-Dtankops.config=$bundle/config.properties" \
    -jar "$bundle/tankops-server.jar" --self-test
arch=$("$bundle/runtime/bin/java" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/os.arch =/ {print $2}')
destination="$project_root/TankOps-Ubuntu-$arch.tar.gz"
tar -czf "$destination" -C "$build_root" TankOps-Ubuntu
echo "Ready: $destination (bundled Linux Java; executable permissions preserved)."
