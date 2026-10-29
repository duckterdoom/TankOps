#!/bin/bash
set -euo pipefail
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
[[ -f "$script_dir/TankOps.jar" ]] || { echo 'Extract the complete TankOps-Mac.zip first.' >&2; exit 1; }
# jpackage creates a runtime for the architecture of this JDK, not a universal app.
build_dir=$(mktemp -d "$script_dir/app-build.XXXXXX")
mkdir -p "$build_dir/input"
cp "$script_dir/TankOps.jar" "$build_dir/input/TankOps.jar"
"$jdk/bin/jpackage" --type app-image --name TankOps --input "$build_dir/input" \
    --main-jar TankOps.jar --main-class tankops.client.Main --dest "$build_dir/output" \
    --app-version 1.0.0 --vendor TankOps --add-modules java.desktop
/usr/bin/ditto -c -k --sequesterRsrc --keepParent "$build_dir/output/TankOps.app" "$build_dir/TankOps-App.zip"
echo "Ready: $build_dir/output/TankOps.app"
echo "Share: $build_dir/TankOps-App.zip (for Macs compatible with this JDK architecture)."
echo 'This app includes Java. It is not Developer ID signed or notarized; see HUONG-DAN.md.'
