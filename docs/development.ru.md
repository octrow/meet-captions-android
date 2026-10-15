# Разработка

[English](development.md) | **Русский**

## Окружение

- JDK 17+ (проверено на 21).
- Android SDK: `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`. Путь — в `local.properties`:
  ```
  sdk.dir=/path/to/android-sdk
  ```
- Gradle ставить не нужно: wrapper (Gradle 9.8.1). AGP 9.4.1 со встроенным Kotlin
  (плагин `org.jetbrains.kotlin.android` не нужен).
- Удобно: [Android CLI](https://developer.android.com/tools) `android` — SDK, эмулятор, `android layout`, `android docs`.
  ```bash
  curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
  android --sdk=/path/to/android-sdk sdk install platforms/android-36 build-tools/36.0.0 platform-tools
  ```

## Сборка и тесты

```bash
./gradlew :app:test :app:assembleDebug        # юнит-тесты и APK
./gradlew :fakemeet:assembleDebug             # тестовый двойник Meet
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Юнит-тесты (`app/src/test`) покрывают склейку: рост текста, правка хвоста, прокрутка, пунктуация и регистр,
недописанное слово, однословный блок, смена говорящего, пропавшее и запоздавшее имя, реальные снимки из Gmail.
Новый случай из реального звонка — сначала тест на снимках из `dump_*.txt`, потом правка.

## Сквозной тест на эмуляторе

`fakemeet/` — приложение с пакетом `com.google.android.apps.tachyon`: показывает скриптованные субтитры двух
говорящих в узлах `speaker_name` и `caption_text`. **Не ставить на телефон с настоящим Meet** — пакеты конфликтуют.

```bash
# эмулятор: см. avdmanager ниже
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r fakemeet/build/outputs/apk/debug/fakemeet-debug.apk
adb shell settings put secure enabled_accessibility_services dev.octrow.meetcaptions/dev.octrow.meetcaptions.CaptionService
adb shell settings put secure accessibility_enabled 1
adb shell am start -n com.google.android.apps.tachyon/dev.octrow.fakemeet.FakeCallActivity
sleep 12; adb shell 'cat /sdcard/Documents/MeetCaptions/*.txt'
```

`adb install -r` и `am start -S` (force-stop) выключают службу: после них повторите обе команды `settings put`.

Ожидается:
```
Аня (…)
Привет, как дела? Начнём

Боб (…)
да, я подготовил цифры
```

Подводные камни эмулятора (headless, Linux):

- Образ `android-36.1 google_apis x86_64` падает в `surfaceflinger` (`hasReadColorBufferDma`) при любом `-gpu`.
  Работает `system-images;android-35;google_apis;x86_64` с `-gpu swiftshader_indirect`.
- Раздел данных по умолчанию 800M — не хватает для установки. Ставьте `disk.dataPartition.size=2G` в `config.ini` AVD.
- AVD на NTFS (fuseblk) нестабилен: падает `system_server`, `/sdcard` не монтируется. Держите AVD на ext4.
- После `sys.boot_completed=1` подождите, пока смонтируется хранилище: `adb shell sm list-volumes` → `emulated;0 mounted`.

```bash
avdmanager create avd -n mc35 -k "system-images;android-35;google_apis;x86_64" -d pixel_6
emulator -avd mc35 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect &
```

Включить отладку без экрана (debug-сборка):

```bash
adb shell "run-as dev.octrow.meetcaptions sh -c 'mkdir -p shared_prefs; printf \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><boolean name=\\\"debug_dump\\\" value=\\\"true\\\" /></map>\" > shared_prefs/prefs.xml'"
adb shell am force-stop dev.octrow.meetcaptions
adb shell settings put secure enabled_accessibility_services dev.octrow.meetcaptions/dev.octrow.meetcaptions.CaptionService
```

## Работа с телефоном по Wi-Fi

1. Телефон: Параметры разработчика → **Беспроводная отладка** → вкл. → «Подключить устройство с помощью кода».
   Окно с кодом не закрывать: порт сопряжения живёт, пока окно открыто.
2. Компьютер:
   ```bash
   adb pair  PHONE_IP:ПОРТ_ИЗ_ОКНА_КОДА  КОД
   adb connect PHONE_IP:ПОРТ_С_ГЛАВНОГО_ЭКРАНА_БЕСПРОВОДНОЙ_ОТЛАДКИ
   ```
   Это два разных порта.
3. Если телефон не отвечает, проверьте маршрут: VPN (`ppp0`) может перехватывать локальную сеть.
   ```bash
   ip route get PHONE_IP            # должно быть dev <wifi-интерфейс>, не ppp0
   sudo ip route add PHONE_IP/32 dev <wifi-интерфейс>   # временно, до перезагрузки или переподключения VPN
   ```
   mDNS-обнаружение (`adb mdns services`) через VPN тоже не работает, порт подключения смотрите на телефоне.

Полезное на живом звонке:

```bash
adb shell dumpsys activity activities | grep -m1 topResumedActivity   # какой пакет держит звонок
adb shell dumpsys accessibility | grep -A2 'Meet Captions'          # служба подключена, флаги
android layout --full -o layout.json                                # дерево экрана с resource-id
android screen capture -o screen.png
adb logcat -d --pid=$(adb shell pidof dev.octrow.meetcaptions)
```

## Версии (октябрь 2026)

AGP 9.4.1, Gradle 9.8.1, Kotlin встроенный в AGP, compileSdk/targetSdk 36, minSdk 30, JUnit 4.13.2.
Переход на AGP 9: убрать плагин `kotlin-android`, `kotlinOptions {}` удалить (jvmTarget берётся
из `compileOptions.targetCompatibility`), Gradle ≥ 9.6, Build Tools ≥ 36.
