# Development

**English** | [Русский](development.ru.md)

## Environment

- JDK 17+ (tested on 21).
- Android SDK: `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`. Set the path in `local.properties`:
  ```
  sdk.dir=/path/to/android-sdk
  ```
- No need to install Gradle: the wrapper is included (Gradle 9.8.1). AGP 9.4.1 with built-in Kotlin
  (the `org.jetbrains.kotlin.android` plugin is not needed).
- Handy: the [Android CLI](https://developer.android.com/tools) `android` for the SDK, emulator, `android layout`, `android docs`.
  ```bash
  curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
  android --sdk=/path/to/android-sdk sdk install platforms/android-36 build-tools/36.0.0 platform-tools
  ```

## Build and tests

```bash
./gradlew :app:test :app:assembleDebug        # юнит-тесты и APK
./gradlew :fakemeet:assembleDebug             # тестовый двойник Meet
```

The first line runs the unit tests and builds the APK, the second builds the Meet test double.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Unit tests (`app/src/test`) cover merging: growing text, a corrected tail, scrolling, punctuation and case,
an unfinished word, a one-word block, speaker change, a missing or late name, and real snapshots from Gmail.
For a new case from a real call, write the test on snapshots from `dump_*.txt` first, then fix the code.

## End-to-end test on an emulator

`fakemeet/` is an app with package `com.google.android.apps.tachyon`. It shows scripted captions from two
speakers in nodes `speaker_name` and `caption_text`. **Do not install it on a phone with the real Meet**: the packages conflict.

```bash
# эмулятор: см. avdmanager ниже
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r fakemeet/build/outputs/apk/debug/fakemeet-debug.apk
adb shell settings put secure enabled_accessibility_services dev.octrow.meetcaptions/dev.octrow.meetcaptions.CaptionService
adb shell settings put secure accessibility_enabled 1
adb shell am start -n com.google.android.apps.tachyon/dev.octrow.fakemeet.FakeCallActivity
sleep 12; adb shell 'cat /sdcard/Documents/MeetCaptions/*.txt'
```

For the emulator itself, see the `avdmanager` commands below.

`adb install -r` and `am start -S` (force-stop) turn the service off: after them, run both `settings put` commands again.

Expected output:
```
Аня (…)
Привет, как дела? Начнём

Боб (…)
да, я подготовил цифры
```

Emulator pitfalls (headless, Linux):

- The `android-36.1 google_apis x86_64` image crashes in `surfaceflinger` (`hasReadColorBufferDma`) with any `-gpu`.
  `system-images;android-35;google_apis;x86_64` with `-gpu swiftshader_indirect` works.
- The default data partition is 800M, which is too small to install the apps. Set `disk.dataPartition.size=2G` in the AVD's `config.ini`.
- An AVD on NTFS (fuseblk) is unstable: `system_server` crashes and `/sdcard` doesn't mount. Keep the AVD on ext4.
- After `sys.boot_completed=1`, wait for storage to mount: `adb shell sm list-volumes` → `emulated;0 mounted`.

```bash
avdmanager create avd -n mc35 -k "system-images;android-35;google_apis;x86_64" -d pixel_6
emulator -avd mc35 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect &
```

Turn on debug mode without touching the screen (debug build):

```bash
adb shell "run-as dev.octrow.meetcaptions sh -c 'mkdir -p shared_prefs; printf \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><boolean name=\\\"debug_dump\\\" value=\\\"true\\\" /></map>\" > shared_prefs/prefs.xml'"
adb shell am force-stop dev.octrow.meetcaptions
adb shell settings put secure enabled_accessibility_services dev.octrow.meetcaptions/dev.octrow.meetcaptions.CaptionService
```

## Working with the phone over Wi-Fi

1. Phone: Developer options → **Wireless debugging** → on → "Pair device with pairing code".
   Keep the code dialog open: the pairing port only lives while it is open.
2. Computer:
   ```bash
   adb pair  PHONE_IP:ПОРТ_ИЗ_ОКНА_КОДА  КОД
   adb connect PHONE_IP:ПОРТ_С_ГЛАВНОГО_ЭКРАНА_БЕСПРОВОДНОЙ_ОТЛАДКИ
   ```
   These are two different ports: `adb pair` takes the port and code from the pairing dialog,
   `adb connect` takes the port shown on the main Wireless debugging screen.
3. If the phone doesn't respond, check the route: a VPN (`ppp0`) can capture the local network.
   ```bash
   ip route get PHONE_IP            # должно быть dev <wifi-интерфейс>, не ppp0
   sudo ip route add PHONE_IP/32 dev <wifi-интерфейс>   # временно, до перезагрузки или переподключения VPN
   ```
   The route should go through your Wi-Fi interface, not `ppp0`. The added route is temporary and is lost on reboot or VPN reconnect.
   mDNS discovery (`adb mdns services`) doesn't work through the VPN either, so read the connect port off the phone.

Useful during a live call:

```bash
adb shell dumpsys activity activities | grep -m1 topResumedActivity   # какой пакет держит звонок
adb shell dumpsys accessibility | grep -A2 'Meet Captions'          # служба подключена, флаги
android layout --full -o layout.json                                # дерево экрана с resource-id
android screen capture -o screen.png
adb logcat -d --pid=$(adb shell pidof dev.octrow.meetcaptions)
```

In order: which package holds the call, whether the service is connected and its flags, the screen tree with resource ids, a screenshot, the app's log.

## Versions (October 2026)

AGP 9.4.1, Gradle 9.8.1, Kotlin built into AGP, compileSdk/targetSdk 36, minSdk 30, JUnit 4.13.2.
Moving to AGP 9: drop the `kotlin-android` plugin, delete `kotlinOptions {}` (jvmTarget comes
from `compileOptions.targetCompatibility`), Gradle ≥ 9.6, Build Tools ≥ 36.
