# Meet Captions for Android

<p align="center"><b>English</b> | <a href="README.ru.md">Русский</a></p>

<p align="center">
<a href="https://github.com/octrow/meet-captions-android/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/octrow/meet-captions-android/actions/workflows/ci.yml/badge.svg"></a>
<a href="https://github.com/octrow/meet-captions-android/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/octrow/meet-captions-android"></a>
<img alt="Android 11+" src="https://img.shields.io/badge/Android-11%2B-3DDC84">
<a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-blue"></a>
</p>

Save Google Meet live captions on Android as text files, with speaker names and timestamps, automatically for
every call. It works when you are not the organizer and in any caption language, including Russian, where
Gemini notes and Meet transcripts are not available.

An accessibility service reads the captions from the call screen (the Meet app or a Meet call inside Gmail) and
writes them to `Documents/MeetCaptions/<date_time>.txt`. No audio is recorded. The app has no internet
permission and no third-party libraries.

```
Вы (21:43:06)
1 2 3 4 5 6 7 8 есть

Dan P (21:43:15)
11 12 14 15 21 проверка
```

<sub>A real test call. «Вы» is Meet's label for you in a Russian-language call.</sub>

Tested on a Samsung S24 Ultra, Android 16, Meet 379 (October 2026).

## Install

1. Download `meet-captions-X.Y.Z.apk` from the [latest release](https://github.com/octrow/meet-captions-android/releases/latest) and open it on the phone.
   For automatic updates, add this repository's URL to [Obtainium](https://github.com/ImranR98/Obtainium).
2. If Play Protect or Samsung Auto Blocker stops the install, see [won't install](docs/troubleshooting.md#wont-install).
3. Developers: `adb install -r meet-captions-X.Y.Z.apk`, or build it yourself, see
   [docs/development.md](docs/development.md).

Apps installed from a file are "restricted": Android greys out their accessibility switch until you allow
restricted settings. The app shows how; details in [service won't turn on](docs/troubleshooting.md#service-wont-turn-on).

## Turn it on

1. Settings → Accessibility → Installed apps → **Meet Captions** → on.
2. Settings → Apps → Meet Captions → Battery → **Unrestricted**, otherwise Samsung puts the service to sleep.
3. In a Meet call: ⋮ → **Captions**, pick the meeting language. Meet remembers the choice.

From then on it runs by itself: while captions are on the call screen, the file is rewritten every 3 seconds.
A new file starts after 5 minutes without captions.

## Good to know

- Only what Meet shows as captions on **your** screen gets saved: the call screen must be open with captions on.
  A minimized call shows no captions, so nothing is saved.
- Captions are Google's speech recognition; its mistakes end up in the file as they are.
- Other participants don't see that you save captions. Check that this is fine for your situation and allowed
  where you live.

## Compared with other options

| Option | Difference |
|---|---|
| Gemini "Take notes for me" and Meet transcripts | Depend on the organizer's Workspace plan; Russian is not a supported language; notes are a summary, not a verbatim transcript. |
| [TranscripTonic](https://github.com/vivek-nexus/transcriptonic) | Chrome extension doing the same on desktop; use it on your computer. Web Meet on a phone redirects to the app. |
| Meeting bots (Vexa and similar) | Join as a visible participant that the organizer has to let in; need a server. |
| Recording audio and transcribing it | Needs consent to record and competes with the call for the microphone. Meet captions are already recognized text. |

More in [docs/decisions.md](docs/decisions.md).

## Transcripts on your computer (Syncthing)

1. Ubuntu: `sudo apt install syncthing && systemctl --user enable --now syncthing`, web UI at http://127.0.0.1:8384.
2. Phone: Syncthing-Fork (F-Droid or Google Play), add the Ubuntu device by QR code or ID.
3. On the phone, share the `Documents/MeetCaptions` folder as "Send only". Accept it on Ubuntu, for example into
   `~/Downloads/TranscripTonic/android`, next to the TranscripTonic transcripts.

## Documentation

| Document | What's in it |
|---|---|
| [docs/decisions.md](docs/decisions.md) | Why it works this way: options considered and rejected |
| [docs/architecture.md](docs/architecture.md) | How the app is built and how captions are merged |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Service won't turn on, no file, `?` instead of names, Meet updated |
| [docs/development.md](docs/development.md) | Build, tests, emulator end-to-end test, working with a phone over adb |

[Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Changelog](CHANGELOG.md) · [MIT license](LICENSE)
· Font: [Golos Text](third_party/golos-text-OFL.txt), SIL OFL 1.1
