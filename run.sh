#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

printf "빌드 중...\r"
./gradlew --console=plain -q installDist 2>/dev/null
printf "              \r"

exec "./build/install/SampleOrderSystem-SunbinKwon-22020106/bin/SampleOrderSystem-SunbinKwon-22020106"
