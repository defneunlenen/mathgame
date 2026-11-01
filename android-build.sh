#!/usr/bin/env bash
# Build the project inside Docker. The only host requirement is Docker.
#
#   ./android-build.sh                 # default: assembleDebug
#   ./android-build.sh test            # unit tests
#   ./android-build.sh build           # everything
#   ./android-build.sh --shell         # shell inside the build image
#
# Gradle caches persist in the "android-agent-gradle" docker volume, and the
# project is bind-mounted, so incremental builds work like a local Gradle run.
set -euo pipefail

IMAGE="android-agent-build"
CACHE_VOLUME="android-agent-gradle"
# Persist the debug signing key so reinstalling over an existing install keeps
# the same signature (otherwise `docker run --rm` regenerates it every build).
ANDROID_HOME_VOLUME="android-agent-android-home"
cd "$(dirname "$0")"

if ! docker image inspect "${IMAGE}" > /dev/null 2>&1; then
  echo "Build image ${IMAGE} not found — building it (first run downloads the Android SDK)..." >&2
  docker build -t "${IMAGE}" . >&2
fi

if [[ "${1:-}" == "--shell" ]]; then
  exec docker run --rm -it \
    -v "${PWD}":/project \
    -v "${CACHE_VOLUME}":/home/builder/.gradle \
    -v "${ANDROID_HOME_VOLUME}":/home/builder/.android \
    --entrypoint bash "${IMAGE}"
fi

# arm64 containers: AGP's embedded aapt2 is x86_64-only. Use the native
# arm64 aapt2 that ships with the SDK build-tools installed in the image.
exec docker run --rm \
  -v "${PWD}":/project \
  -v "${CACHE_VOLUME}":/home/builder/.gradle \
  -v "${ANDROID_HOME_VOLUME}":/home/builder/.android \
  "${IMAGE}" \
  "-Pandroid.aapt2FromMavenOverride=${AAPT2_OVERRIDE:-/opt/android-sdk/build-tools/36.0.0/aapt2}" \
  "$@"
