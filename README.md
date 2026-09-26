# DuoCam

DuoCam is a local-first Android camera project focused on real camera capture, truthful device capability reporting, teleprompter scripts, and local media workflows.

## Current product boundary

The V1 codebase is being hardened around features that can be verified on-device:

- Camera capture and recording.
- Front/back camera capability handling.
- Teleprompter scripts.
- Local gallery, playback, sharing, and deletion.
- Release signing that keeps keystores and passwords outside the repository.

Cloud backup, billing, and media-editing features should only be exposed when their underlying integrations are implemented and verified.

## Build and verify

See [BUILDING.md](BUILDING.md) for prerequisites and signing details.

Typical validation:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

On Windows, use `gradlew.bat`.

## Security

Do not commit release keystores, signing passwords, `local.properties`, environment files, or generated build logs.
