# MaaHatsuboshiTA Android

Native Android companion/controller project inspired by [Carpenter-MK1/MaaHatsuboshiTA](https://github.com/Carpenter-MK1/MaaHatsuboshiTA).

> **Status: early foundation — not yet a functional ADB controller.** The current app does not pair with or control a second Android device.

## Architecture

- **Kotlin + Jetpack Compose** — Android application and dashboard.
- **C++ + JNI + CMake** — initial native library integration.
- **Rust** — platform-independent task-state core.
- **GitHub Actions** — Rust tests and Android debug APK workflow.

## Implemented

- Compose dashboard with target IP, ADB port, wireless pairing port, and pairing-code fields.
- Initial C++ JNI library.
- Rust task-state model with start/pause handling and unit tests.
- CI workflow for Rust tests, Android debug build, APK checksum verification, and artifact upload.

## Not implemented

- Wireless ADB pairing, authentication, or network transport.
- Device session management and reconnect logic.
- Remote screen capture or streaming.
- Touch/key input forwarding.
- OCR, template matching, or game recognition.
- Daily, Contest, Farm New Account, or other automation pipelines.

The **Connect** button is currently a placeholder. Treat this app as a development foundation, not a working remote-control or game-automation tool.

## Build locally

Requirements: JDK 17, Android SDK, Gradle compatible with Android Gradle Plugin 8.7.3, and Rust toolchain for Rust tests.

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Run Rust tests:

```bash
cd rust
cargo test --locked
```

## CI and artifacts

See [GitHub Actions](https://github.com/game6789y-wq/Gaku-bot/actions). A debug APK is available only after a successful workflow run, verification, and artifact upload. A debug artifact is not a production release.

## Roadmap

1. Implement secure Wireless Debugging pairing and authenticated ADB transport.
2. Add connection state, timeout, disconnect, and reconnect handling.
3. Implement authorized screen capture and input forwarding.
4. Add OCR and template matching.
5. Port automation pipelines after reviewing upstream licenses and dependencies.
6. Test on real devices and document verified compatibility.

## Security and licensing

Only connect to devices you own or are authorized to control. Avoid logging or storing pairing codes, credentials, or private screen content.

This project is inspired by the upstream project. Review its license and all third-party notices before reusing source code, resources, models, binaries, or framework components. No upstream automation assets are claimed to be included in this foundation.
