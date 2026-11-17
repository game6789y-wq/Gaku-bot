# MaaHatsuboshiTA Android

Native Android companion/controller project inspired by [Carpenter-MK1/MaaHatsuboshiTA](https://github.com/Carpenter-MK1/MaaHatsuboshiTA).

> **Status: early foundation — not yet a functional ADB controller.** The app can scan the local network for Android Wireless Debugging service advertisements, but it cannot pair, authenticate, or control a second device.

## Architecture

- **Kotlin + Jetpack Compose** — Android application and dashboard.
- **Kadb** — ADB Wireless Debugging pairing, authenticated transport, and read-only shell probe.
- **Android Keystore + AES-256-GCM** — wraps the persisted ADB RSA host identity at rest.
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
- ADB-compatible RSA-2048 host identity persisted as encrypted private-key material. An AES-256-GCM wrapping key is held in Android Keystore; a SHA-256 certificate fingerprint is displayed in the dashboard.
- Wireless ADB pairing action using the target device's manually entered six-digit pairing code.
- Authenticated ADB TLS connection action using the separate connect port, followed by a read-only `getprop ro.product.model` probe.
- Rust task-state machine with start, pause, resume, complete, and fail transitions plus unit tests.
- CI workflow for Rust tests, Android debug build, APK checksum verification, and artifact upload.

## Not implemented

- Pairing and a read-only authenticated connection are now wired through Kadb, but still require validation on real Android devices and different OEM builds.
- Persistent connection/session UI, disconnect, timeout, and reconnect handling.
- Remote screen capture or streaming.
- Touch/key input forwarding.
- OCR, template matching, or game recognition.
- Daily, Contest, Farm New Account, or other automation pipelines.

Network discovery is not pairing. A service appearing in the list only means that its DNS-SD advertisement was found on the local network. The app stores the ADB RSA private key encrypted with AES-GCM. The AES wrapping key is non-exportable in Android Keystore; the RSA private key is decrypted in memory only when Kadb performs ADB operations. The user must enter the pairing code displayed by the target device and must enable Wireless Debugging there. The app does not obtain or infer the pairing code.

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

1. Validate Kadb pairing, TLS authentication, and read-only shell probe on real Android devices.
2. Add persistent connection state, timeout, disconnect, and reconnect handling.
3. Implement authorized screen capture and input forwarding.
4. Add OCR and template matching.
5. Port automation pipelines after reviewing upstream licenses and dependencies.
6. Test on multiple OEM builds and document verified compatibility.

## Security and licensing

Kadb is an Apache-2.0 licensed third-party dependency: https://github.com/flyfishxu/Kadb. The app uses an Android Keystore AES wrapping key to encrypt the persisted ADB host private key. The unwrapped RSA key exists in memory during protocol operations. Only connect to devices you own or are authorized to control. Never log or commit pairing codes, ADB private keys, credentials, or private screen content.

This project is inspired by the upstream project. Review its license and all third-party notices before reusing source code, resources, models, binaries, or framework components. No upstream automation assets are claimed to be included in this foundation.
