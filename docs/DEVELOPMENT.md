# Development setup

## Requirements

- **Android Studio** (bundles the Android SDK), or a standalone Android SDK with `ANDROID_HOME`
  set.
- **A JDK in the 17–21 range** to run Gradle. The Gradle wrapper (8.10.2, required by Android
  Gradle Plugin 8.7.3) does not support newer JDKs, and the JDK that your system or Android Studio
  provides may be newer. Install one with whatever you prefer (SDKMAN, Homebrew, your OS package
  manager, an installer) and point `JAVA_HOME` at it.
- An emulator or a physical device with USB debugging, for running the app.

## Build and run

```bash
export JAVA_HOME=/path/to/jdk-17-to-21
export ANDROID_HOME=/path/to/android/sdk    # often ~/Library/Android/sdk or ~/Android/Sdk

./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # build and install on the connected device/emulator
```

Or open the project in Android Studio, create a virtual device (Device Manager → Create Device) and
press Run. If Android Studio's Gradle JDK is out of range, set it under Settings → Build Tools →
Gradle → Gradle JDK.

On first launch, grant the permissions and accept the call-screening role prompt. Without the role
no call is ever delivered to the app.

## Checks

```bash
./gradlew ktlintCheck detekt lintDebug      # static analysis
./gradlew testDebugUnitTest                 # unit tests (JVM, no emulator)
./gradlew ktlintFormat                      # auto-fix style
```

See [TESTING.md](TESTING.md) for manual and device-level testing.

## Build notes

- Kotlin 2.0 needs the separate `org.jetbrains.kotlin.plugin.compose` Gradle plugin; Compose no
  longer works through `composeOptions.kotlinCompilerExtensionVersion` alone.
- `buildFeatures.buildConfig = true` is set explicitly (an AGP 8.x opt-in). The Debug tab is gated
  on `BuildConfig.DEBUG`.
- Dependency versions live in `gradle/libs.versions.toml`.

## Signed release builds

The `release` workflow builds on `v*.*.*` tags. Locally, release signing reads the
`CALLMANAGER_RELEASE_*` Gradle properties (see `app/build.gradle.kts`); never commit keystores or
passwords.

## Troubleshooting

- **`./gradlew` fails immediately with a Java version error:** your `JAVA_HOME` or default `java`
  is too new. Point `JAVA_HOME` at a JDK 17–21.
- **Robolectric tests fail with an SDK version error:** `CallRepositoryTest` is pinned to
  `@Config(sdk = [34])` because Robolectric 4.13 doesn't support this project's `compileSdk` (35).
  Don't remove the pin without upgrading Robolectric.
- **Calls aren't screened on a real device:** check the battery-optimization prompt in the Rules
  tab. Some OEMs (Xiaomi, Samsung, Huawei, others) kill the app hard enough that Android can't wake
  it for a call. Exempting the app fixes this without a foreground service. Also confirm that Call
  Manager is still the selected call-screening app in Android's default apps settings.
