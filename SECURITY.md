# Security

**English** | [Русский](SECURITY.ru.md)

## Reporting a vulnerability

Please report privately through [GitHub private vulnerability reporting](https://github.com/octrow/meet-captions-android/security/advisories/new),
not in a public issue. I'll reply within a week.

Only the latest release is supported.

## What the app can access

Meet Captions is an accessibility service, so Android lets it read the screen of the apps it listens to.
It limits itself to Google Meet and Gmail (`packageNames` in
[accessibility_service.xml](app/src/main/res/xml/accessibility_service.xml)) and writes text files to
`Documents/MeetCaptions`. The app has no `INTERNET` permission
([AndroidManifest.xml](app/src/main/AndroidManifest.xml)), so it can't send anything anywhere.

## Checking a release APK

Every release lists the SHA-256 of the APK in `SHA256SUMS`. Once the repository is public, each APK also has a
build provenance attestation:

```bash
gh attestation verify meet-captions-X.Y.Z.apk -R octrow/meet-captions-android
```
