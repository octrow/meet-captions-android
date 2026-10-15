# Android (Samsung S24 Ultra): Meet Captions

Служба специальных возможностей читает субтитры с экрана приложения Google Meet и сохраняет их в
`Documents/MeetCaptions/<дата_время>.txt`. Звук не пишется, сеть не используется, зависимостей нет.

```
Аня (14:03:12)
привет, начнём с отчёта

Боб (14:03:20)
да, я подготовил цифры
```
Новая встреча (файл) начинается, если субтитров не было 5 минут. Пока идут субтитры, файл перезаписывается каждые 3 с.

## Сборка и установка

AGP 9.4 (встроенный Kotlin), Gradle 9.8 через wrapper, JDK 17+, `local.properties` → `sdk.dir=…`.

```bash
./gradlew :app:test :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Сквозной тест без Meet (эмулятор)

`fakemeet/` — тестовый двойник: приложение с пакетом Meet (`com.google.android.apps.tachyon`), которое
показывает скриптованные субтитры двух говорящих (рост текста, правка пунктуации, смена имени).
На телефон с настоящим Meet его не ставить: пакеты конфликтуют.

```bash
./gradlew :fakemeet:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r fakemeet/build/outputs/apk/debug/fakemeet-debug.apk
adb shell settings put secure enabled_accessibility_services dev.octrow.meetcaptions/dev.octrow.meetcaptions.CaptionService
adb shell settings put secure accessibility_enabled 1
adb shell am start -n com.google.android.apps.tachyon/dev.octrow.fakemeet.FakeCallActivity
sleep 12; adb shell 'cat /sdcard/Documents/MeetCaptions/*.txt'
```

Ожидается две реплики: `Аня … Привет, как дела? Начнём` и `Боб … да, я подготовил цифры`.
Эмулятор с образом android-36.1 google_apis падает в surfaceflinger без окна; образ android-35 работает.

## Включение на телефоне

1. Настройки → Специальные возможности → Установленные приложения → **Meet Captions** → вкл.
   Если пункт серый («Ограниченная настройка»): Настройки → Приложения → Meet Captions → ⋮ →
   **Разрешить ограниченные настройки**, потом снова п. 1.
2. Настройки → Приложения → Meet Captions → Батарея → **Без ограничений** (иначе Samsung усыпит службу).
3. В Meet во время звонка: ⋮ → Субтитры → язык **русский**.

## Первая настройка: id узлов субтитров

Как Meet называет view субтитров, заранее неизвестно. По умолчанию текст ищется по id с `caption`,
имя говорящего по id с `speaker|participant_name`. Если файла нет или вместо имён стоит `?`:

1. Откройте Meet Captions → включите «Отладка».
2. Зайдите в звонок, включите субтитры, пусть кто-нибудь поговорит минуту; выключите отладку.
3. `adb pull /sdcard/Documents/MeetCaptions/`: `events_*.txt` — все события Meet (тип, id источника, текст),
   `dump_*.txt` раз в 5 с — список всех окон и дерево окон Meet. Найдите строки с текстом субтитров и
   именем говорящего, посмотрите их `id=`. Или просто пришлите файлы.
   В дампах есть e-mail аккаунта и имена участников: после разбора удалите их.
4. Впишите части этих id в два поля regex → «Сохранить regex».

Если у текста субтитров `id=null` (Compose без resource id), regex не поможет: пришлите дамп, нужен другой селектор.

## Как склеиваются субтитры

`CaptionMerger`: каждый снимок области субтитров ищется по первым трём словам в текущем блоке; всё до совпадения
уже уехало вверх и сохраняется, остальное заменяется снимком (так учитываются и дописанные, и исправленные слова).
Нет совпадения: блок закрывается, начинается новый абзац.
`Transcript`: новые слова есть только в нижнем блоке; смена имени в нём начинает новую реплику со временем.
