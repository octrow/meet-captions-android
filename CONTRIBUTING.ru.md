# Как помочь проекту

[English](CONTRIBUTING.md) | **Русский**

Баг-репорты и pull request'ы приветствуются. Meet часто меняет экран звонка, поэтому отчёт с телефона или версии Meet,
которых у меня нет, полезнее всего.

## Сообщить об ошибке

Заполните [форму бага](https://github.com/octrow/meet-captions-android/issues/new?template=bug.yml). Если после
обновления Meet субтитры перестали сохраняться, включите в приложении «Отладка» → «Записывать события и дампы окон», сделайте короткий звонок
с субтитрами и приложите файлы `dump_*.txt` и `events_*.txt` из `Documents/MeetCaptions`. Сначала уберите из них
личное: там текст субтитров и имена участников.

## Изменить код

Окружение, тесты и сквозной тест на эмуляторе описаны в [docs/development.ru.md](docs/development.ru.md).

```bash
./gradlew :app:test :app:lintDebug :app:assembleDebug
```

- Исправление по реальному звонку начинается с падающего теста на дампе этого звонка (см. `CaptionMergerTest`).
- Новые зависимости только с серьёзной причиной: кроме JUnit их нет, и это намеренно.
- Коммиты: `<type>: <description>`, типы `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`.

## Релиз

Один раз: создать ключ подписи и сохранить его в секретах репозитория. Файл `.jks` и пароли храните вне репозитория:
без них пользователи не смогут обновиться поверх старой установки.

```bash
keytool -genkeypair -v -keystore meet-captions.jks -alias meet-captions -keyalg RSA -keysize 4096 -validity 10000
gh secret set SIGNING_KEYSTORE_BASE64 < <(base64 -w0 meet-captions.jks)
gh secret set SIGNING_KEY_ALIAS --body meet-captions
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_PASSWORD
```

Каждый релиз:

1. Увеличить `versionCode` (+1) и `versionName` в [app/build.gradle.kts](app/build.gradle.kts).
2. Записать изменения в [CHANGELOG.md](CHANGELOG.md) и `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.
3. Закоммитить, поставить тег `vX.Y.Z` (равен `versionName`), запушить тег. Workflow `release` соберёт подписанный
   APK и опубликует релиз на GitHub.
