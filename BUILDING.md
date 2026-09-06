# Building DuoCam

## Local prerequisites

- JDK 17 or newer supported by the selected Android Gradle Plugin. JDK 21 is
  used by the current development environment.
- Android SDK platform 36.1 and Build Tools 36.0.0.
- No system Gradle installation is required; use the committed Gradle Wrapper.

On Windows, set `ANDROID_HOME` to the Android SDK directory if Android Studio
has not already configured it:

```powershell
$env:ANDROID_HOME = "C:\\Android\\Sdk"
```

## Validation commands

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

The debug variant uses Android's standard debug signing configuration and does
not require a keystore in the repository.

## Release signing

Release credentials must remain outside version control. Configure all of the
following before requesting a release task:

```powershell
$env:KEYSTORE_PATH = "C:\\secure\\duocam-upload.jks"
$env:STORE_PASSWORD = "..."
$env:KEY_PASSWORD = "..."
.\gradlew.bat bundleRelease
```

The release build fails with a clear configuration error when the keystore or
credentials are missing. Never commit the keystore, passwords, or generated
`local.properties` file.

## Current product boundary

The app is being hardened toward a local-first V1: genuine camera capture,
truthful device capability reporting, teleprompter scripts, local gallery,
playback, share, and delete. Cloud backup, billing, and media editing must not
be exposed unless their underlying integrations are real and verified.
