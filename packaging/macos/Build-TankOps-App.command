#!/bin/bash
set -euo pipefail
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd -- "$script_dir"
[[ "$(uname -s)" == Darwin ]] || { echo 'Build this app on macOS.' >&2; exit 1; }
if [[ -n "${JAVA_HOME:-}" ]]; then
    jdk="$JAVA_HOME"
else
    jdk=$(/usr/libexec/java_home -v 21+)
fi
[[ -x "$jdk/bin/jpackage" ]] || { echo 'Install a full macOS JDK 21+ (including jpackage).' >&2; exit 1; }
java_version=$("$jdk/bin/java" -version 2>&1)
if [[ ! "$java_version" =~ version\ \"([0-9]+) ]] || (( BASH_REMATCH[1] < 21 )); then
    echo 'JDK 21 or newer is required. Check JAVA_HOME.' >&2
    exit 1
fi
source_jar="${TANKOPS_JAR:-$script_dir/TankOps.jar}"
[[ -f "$source_jar" ]] || { echo 'Extract the complete TankOps-Mac.zip first.' >&2; exit 1; }
# jpackage creates a runtime for the architecture of this JDK, not a universal app.
output_root="${TANKOPS_OUTPUT_DIR:-$script_dir}"
mkdir -p "$output_root"
build_dir=$(mktemp -d "$output_root/app-build.XXXXXX")
mkdir -p "$build_dir/input"
cp "$source_jar" "$build_dir/input/TankOps.jar"
modules=$("$jdk/bin/jdeps" --print-module-deps "$build_dir/input/TankOps.jar")
"$jdk/bin/jlink" --module-path "$jdk/jmods" --add-modules "$modules,jdk.accessibility" \
    --strip-debug --no-header-files --no-man-pages --output "$build_dir/runtime"
"$jdk/bin/jpackage" --type app-image --name TankOps --input "$build_dir/input" \
    --main-jar TankOps.jar --main-class tankops.client.Main --dest "$build_dir/output" \
    --app-version 1.0.0 --vendor TankOps --runtime-image "$build_dir/runtime"
app="$build_dir/output/TankOps.app"
# Test the actual app launcher with external Java and JVM options removed.
env -u JAVA_HOME -u JAVA_TOOL_OPTIONS -u _JAVA_OPTIONS -u JDK_JAVA_OPTIONS \
    PATH=/usr/bin:/bin:/usr/sbin:/sbin "$app/Contents/MacOS/TankOps" --self-test
/usr/bin/ditto -c -k --sequesterRsrc --keepParent "$build_dir/output/TankOps.app" "$build_dir/TankOps-App.zip"
arch=$("$jdk/bin/java" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/os.arch =/ {print $2}')
share_zip="$output_root/TankOps-Mac-$arch.zip"
cp "$build_dir/TankOps-App.zip" "$share_zip"
echo "Ready: $build_dir/output/TankOps.app"
echo "Share: $share_zip (for Macs compatible with this JDK architecture)."
echo 'This app includes Java. It is not Developer ID signed or notarized; see HUONG-DAN.md.'
