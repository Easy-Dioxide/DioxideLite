#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# DioxideLite 构建脚本（macOS / Windows / Linux 通用）
#
# 用法：
#   ./scripts/build-macos.sh                 # 本机平台包（原生库按构建机自动探测）
#   ./scripts/build-macos.sh --arm64-only    # 只打 Apple Silicon 原生库（体积更小）
#   ./scripts/build-macos.sh --all-platforms # 三平台通用包（Win x64 + Linux x64 + macOS arm64/x64）
#   ./scripts/build-macos.sh --run           # 构建完直接启动 dev 客户端
#
# 依赖：
#   * JDK 25（项目 java_version=25，class 文件版本 69，21 及以下无法编译/运行）
#     可用 JAVA_HOME 指定，或放在 ~/jdks/jdk-25*/Contents/Home
#   * 首次构建需要联网（Maven Central / maven.fabricmc.net / jitpack）
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."

PLATFORMS=""          # 空 = 按构建机自动探测
WEBRTC=""             # 空 = 按构建机自动探测
RUN_CLIENT=0
for arg in "$@"; do
    case "$arg" in
        --arm64-only)    PLATFORMS="skija-macos-arm64" ;;
        --all-platforms) PLATFORMS="skija-windows-x64,skija-linux-x64,skija-macos-arm64,skija-macos-x64"
                         WEBRTC="windows-x86_64,linux-x86_64,macos-aarch64,macos-x86_64" ;;
        --run)           RUN_CLIENT=1 ;;
        *) echo "unknown option: $arg" >&2; exit 2 ;;
    esac
done

if [[ -z "${JAVA_HOME:-}" ]]; then
    for candidate in "$HOME"/jdks/jdk-25*/Contents/Home /Library/Java/JavaVirtualMachines/temurin-25*/Contents/Home; do
        if [[ -x "$candidate/bin/java" ]]; then
            JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
    echo "找不到 JDK 25，请先安装 Temurin 25 并设置 JAVA_HOME" >&2
    exit 1
fi

ARGS=()
[[ -n "$PLATFORMS" ]] && ARGS+=("-Pskija_platforms=$PLATFORMS")
[[ -n "$WEBRTC" ]]    && ARGS+=("-Pwebrtc_platforms=$WEBRTC")

echo "JAVA_HOME     = $JAVA_HOME"
"$JAVA_HOME/bin/java" -version
echo "skija 原生库   = ${PLATFORMS:-（按构建机自动探测）}"
echo "webrtc 原生库  = ${WEBRTC:-（按构建机自动探测）}"

if [[ "$RUN_CLIENT" == "1" ]]; then
    exec ./gradlew runClient ${ARGS[@]+"${ARGS[@]}"}
fi

./gradlew clean build ${ARGS[@]+"${ARGS[@]}"}

VERSION="$(sed -n 's/^version=//p' gradle.properties)"
JAR="build/libs/DioxideLite-${VERSION}.jar"
SUFFIX="macos"
[[ "$PLATFORMS" == *"linux"* || -z "$PLATFORMS" ]] && SUFFIX="platform"
[[ "$PLATFORMS" == *"linux"* && "$PLATFORMS" == *"windows"* ]] && SUFFIX="universal"
mkdir -p dist
cp "$JAR" "dist/DioxideLite-${VERSION}-${SUFFIX}.jar"
echo
echo "产物: dist/DioxideLite-${VERSION}-${SUFFIX}.jar"
shasum -a 256 "$JAR" "dist/DioxideLite-${VERSION}-${SUFFIX}.jar"
