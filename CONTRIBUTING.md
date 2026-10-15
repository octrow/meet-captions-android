# Contributing

**English** | [Русский](CONTRIBUTING.ru.md)

Bug reports and pull requests are welcome. Meet changes its screen often, so a report from a phone or Meet
version I don't have is the most useful thing you can send.

## Reporting a bug

Use the [bug form](https://github.com/octrow/meet-captions-android/issues/new?template=bug.yml). If captions stop
being saved after a Meet update, open the app, section «Отладка» (Debug), and turn on «Записывать события и дампы окон» (record events and window dumps), make a short call with captions on, and
attach the `dump_*.txt` and `events_*.txt` files from `Documents/MeetCaptions`. Remove anything private from them
first: they contain the caption text and participant names.

## Changing the code

Setup, tests and the emulator end-to-end test are in [docs/development.md](docs/development.md).

```bash
./gradlew :app:test :app:lintDebug :app:assembleDebug
```

- A fix for a real call starts with a failing test built from that call's dump (see `CaptionMergerTest`).
- No new dependencies without a good reason: the app has none besides JUnit, and that's on purpose.
- Commits: `<type>: <description>` with `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`.

## Releasing

One-time: create a signing key and store it as repository secrets. Keep the `.jks` file and its passwords
outside the repository; losing them means users can't update over the old install.

```bash
keytool -genkeypair -v -keystore meet-captions.jks -alias meet-captions -keyalg RSA -keysize 4096 -validity 10000
gh secret set SIGNING_KEYSTORE_BASE64 < <(base64 -w0 meet-captions.jks)
gh secret set SIGNING_KEY_ALIAS --body meet-captions
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_PASSWORD
```

Each release:

1. Bump `versionCode` (+1) and `versionName` in [app/build.gradle.kts](app/build.gradle.kts).
2. Add the changes to [CHANGELOG.md](CHANGELOG.md) and `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.
3. Commit, tag `vX.Y.Z` (must equal `versionName`), push the tag. The `release` workflow builds the signed APK
   and publishes the GitHub release.
