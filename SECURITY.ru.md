# Безопасность

[English](SECURITY.md) | **Русский**

## Как сообщить об уязвимости

Пишите приватно через [GitHub private vulnerability reporting](https://github.com/octrow/meet-captions-android/security/advisories/new),
а не в открытый issue. Отвечу в течение недели.

Поддерживается только последний релиз.

## К чему у приложения есть доступ

Meet Captions — служба специальных возможностей, поэтому Android даёт ей читать экран приложений, которые она слушает.
Она ограничена Google Meet и Gmail (`packageNames` в
[accessibility_service.xml](app/src/main/res/xml/accessibility_service.xml)) и пишет текстовые файлы в
`Documents/MeetCaptions`. Разрешения `INTERNET` у приложения нет
([AndroidManifest.xml](app/src/main/AndroidManifest.xml)), поэтому отправить что-то в сеть оно не может.

## Проверка APK из релиза

В каждом релизе есть `SHA256SUMS` с хешем APK. Когда репозиторий станет публичным, у каждого APK появится
аттестация сборки:

```bash
gh attestation verify meet-captions-X.Y.Z.apk -R octrow/meet-captions-android
```
