# MaaHatsuboshiTA Android

Native Android companion/controller project inspired by [Carpenter-MK1/MaaHatsuboshiTA](https://github.com/Carpenter-MK1/MaaHatsuboshiTA).

> **Status: early foundation — not yet a functional ADB controller.** The app can scan the local network for Android Wireless Debugging service advertisements, but it cannot pair, authenticate, or control a second device.

## Architecture

- **Kotlin + Jetpack Compose** — Android application and dashboard.
- **Android NsdManager (DNS-SD/mDNS)** — discovery of ADB Wireless Debugging pairing and connect service advertisements.
- **C++ + JNI + CMake** — initial native library integration.
- **Rust** — platform-independent task-state core.
- **GitHub Actions** — Rust tests and Android debug APK workflow.

## Implemented

- Compose dashboard with target IP, ADB port, wireless pairing port, and pairing-code fields.
- Local IPv4, port-range, and six-digit pairing-code validation (validation only).
- Local network discovery for `_adb-tls-pairing._tcp` and `_adb-tls-connect._tcp` DNS-SD services.
- Displays discovered service address, port, and service name.
- Tap a discovered pairing or connect endpoint to fill the matching manual field; pairing and connect ports remain separate.
- Removes endpoints from the list when Android reports that their DNS-SD service disappeared.
- C++ JNI foundation.
- Rust task-state machine with start, pause, resume, complete, and fail transitions plus unit tests.
- CI workflow for Rust tests, Android debug build, APK checksum verification, and artifact upload.

## Not implemented

- ADB TLS pairing handshake and key management.
- Authenticated ADB transport or session management.
- Remote screen capture or streaming.
- Touch/key input forwarding.
- OCR, template matching, or game recognition.
- Daily, Contest, Farm New Account, or other automation pipelines.

Network discovery is not pairing. A service appearing in the list only means that its DNS-SD advertisement was found on the local network. The user must still approve pairing on the target device through Android Wireless Debugging. The app does not obtain or infer the pairing code.

## Test on devices

1. Install the debug APK on the controller Android device.
2. Connect controller and target to the same Wi-Fi network.
3. On the target, open Developer options → Wireless debugging.
4. Start scanning in the app.
5. Check whether pairing/connect service advertisements appear.

Some routers isolate Wi-Fi clients or block multicast DNS; in that case discovery may not work even when both devices have internet access.

## Build locally

Requirements: JDK 17, Android SDK, Gradle compatible with Android Gradle Plugin 8.7.3, and Rust toolchain for Rust tests.

```bash
gradle --no-daemon assembleDebug
cd rust && cargo test --locked
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## CI and artifacts

See [GitHub Actions](https://github.com/game6789y-wq/Gaku-bot/actions). A debug APK is available only after a successful workflow run, verification, and artifact upload. A debug artifact is not a production release.

## Roadmap

1. Verify endpoint selection and service-loss handling on real Android devices.
2. Implement secure Wireless Debugging pairing and authenticated ADB transport.
3. Add connection state, timeout, disconnect, and reconnect handling.
4. Implement authorized screen capture and input forwarding.
5. Add OCR and template matching.
6. Port automation pipelines after reviewing upstream licenses and dependencies.
7. Test on real devices and document verified compatibility.

## Security and licensing

Only connect to devices you own or are authorized to control. Never log or commit pairing codes, ADB private keys, credentials, or private screen content.

This project is inspired by the upstream project. Review its license and all third-party notices before reusing source code, resources, models, binaries, or framework components. No upstream automation assets are claimed to be included in this foundation.
