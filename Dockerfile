# ==============================================================================
# android-agent — self-contained Android build environment.
#
# Nothing is required on the host except Docker. The project is mounted as a
# volume and Gradle runs inside this image.
#
#   docker build -t android-agent-build .
#   ./android-build.sh [gradle tasks...]
#
# Prefer the helper script: ./android-build.sh [gradle tasks...]
# ==============================================================================
# amd64 base: AGP's aapt2 tooling is x86_64-only for linux; on Apple Silicon
# containers this runs under Rosetta translation transparently.
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-noble

ARG CMDLINE_TOOLS_BUILD=15859902
ARG GRADLE_VERSION=8.14.3
ARG PLATFORM=android-36
ARG BUILD_TOOLS=36.0.0
# AGP downloads its own default build-tools revision at sync time unless it is
# already present; ship it here so builds never need to write to the SDK.
ARG BUILD_TOOLS_LEGACY=35.0.0

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_USER_HOME=/home/builder/.gradle

# Base tools ---------------------------------------------------------------
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

# Gradle (distribution version must match gradle/wrapper in the project) ----
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

# Non-root builder user ------------------------------------------------------
# NB: the ubuntu noble base already occupies UID 1000 (user "ubuntu"), so the
# builder user must use a different id. The cache directory is created up-front
# so the named volume inherits builder ownership on first mount.
RUN useradd -m -u 1001 builder \
    && mkdir -p "${GRADLE_USER_HOME}" \
    && chown -R builder "${GRADLE_USER_HOME}"

# Android SDK command-line tools + packages ----------------------------------
# Installed as root, then handed to builder in the same layer (no copy-up), so
# the SDK stays writable if AGP ever needs to fetch another component.
RUN curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip" -o /tmp/clt.zip \
    && mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && unzip -q /tmp/clt.zip -d /tmp/clt \
    && mv /tmp/clt/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm -rf /tmp/clt /tmp/clt.zip \
    && export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${PATH}" \
    && yes | sdkmanager --licenses > /dev/null 2>&1 || true \
    && sdkmanager --install \
        "platforms;${PLATFORM}" \
        "build-tools;${BUILD_TOOLS}" \
        "build-tools;${BUILD_TOOLS_LEGACY}" \
        "platform-tools" > /dev/null \
    && chown -R builder "${ANDROID_HOME}"

ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Persisted debug-keystore directory (mounted as a named volume by
# android-build.sh); pre-created with the right owner so the volume inherits it.
RUN mkdir -p /home/builder/.android && chown builder /home/builder/.android

USER builder
WORKDIR /project

ENTRYPOINT ["gradle", "--no-daemon", "--max-workers=4"]
CMD ["assembleDebug"]
