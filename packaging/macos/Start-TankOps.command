#!/bin/bash
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd -- "$script_dir"

fail() {
    printf '%s\n' "$1" 'See HUONG-DAN.md. Press Enter to close.'
    read -r _ || true
    exit 1
}

[[ "$(uname -s)" == Darwin ]] || fail 'This launcher is for macOS.'
[[ -f "$script_dir/TankOps.jar" ]] || fail 'TankOps.jar is missing. Extract the entire ZIP first.'
java_bin=''
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    java_bin="$JAVA_HOME/bin/java"
elif java_home=$(/usr/libexec/java_home -v 21+ 2>/dev/null); then
    java_bin="$java_home/bin/java"
fi
[[ -n "$java_bin" ]] || fail 'Install Java 21 or newer for your Mac first (Temurin JDK 21).'
java_version=$("$java_bin" -version 2>&1) || fail 'Java could not start.'
if [[ ! "$java_version" =~ version\ \"([0-9]+) ]] || (( BASH_REMATCH[1] < 21 )); then
    fail 'Java 21 or newer is required. Check JAVA_HOME if it points to an older installation.'
fi
if "$java_bin" -Xdock:name=TankOps -jar "$script_dir/TankOps.jar"; then
    exit 0
else
    fail 'TankOps exited with an error. Copy the terminal output for troubleshooting.'
fi
