# AGENTS.md

NFC Aime Reader: Android phone reads Aime/FeliCa cards and feeds a SEGA arcade game (via segatools) over WebSocket.

## Repo layout (three independent components)

- `NfcAimeReaderDLL/` — C#/.NET 9 **aimeio hook DLL** that segatools loads into the game. Uses Fleck (WebSocket server) + CsWin32.
- `NfcAimeReader.Android/` — Android app (Kotlin + Compose + Hilt) that scans cards and sends them to the DLL.
- `aimeio-multi/` — a **separate, alternative** C (Meson) aimeio DLL for multiple cards. Do NOT confuse it with the C# DLL; it uses its own WebSocket port (8080) and protocol.

## WebSocket protocol (DLL ← Android)

Request (`WebSocketClient.kt` / `DllMain.cs` / `CONTRIBUTING.md`):
```json
{ "module": "card", "function": "insert", "params": "<hex>" }
```
Response: `{ "status": true }`.

`params` length decides mode in `NfcAimeReaderDLL/Card.cs:26`:
- 16 hex chars → FeliCa **IDm** mode (`SetCardIdm`)
- 20 hex chars → **AccessCode** mode (`SetCardAccessCode`)

## Build commands

.NET DLL (matches CI `build_release.yml`):
```
dotnet publish -c Release -r win-x64 -p:Version=<ver> NfcAimeReaderDLL/NfcAimeReaderDLL.csproj
```
Output: `NfcAimeReaderDLL/bin/Release/net9.0/win-x64/publish/NfcAimeReaderDLL.dll`.
Note the csproj sets `<PublishAot>true</PublishAot>`; `NativeMethods.txt` is CsWin32 input (methods it generates), not hand-written code. `Config.cs` reads `segatools.ini` `[aimeio]` section for `serverAddress` (default `0.0.0.0`) / `serverPort` (default `14514`).

Android APK (CI does this on Linux with JDK 17):
```
./gradlew assembleRelease -PversionCode=<code> -PversionName=<ver>
```
`versionCode`/`versionName` come from Gradle `-P` properties (`app/build.gradle.kts:20`), not from the manifest. Release signing requires a local `app/signing.properties` (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) which is only loaded if present.

aimeio-multi C DLL (cross-compiled from Linux):
```
meson setup --cross cross-mingw-32.txt b32 && ninja -C b32
meson setup --cross cross-mingw-64.txt b64 && ninja -C b64
```
Requires Meson + MinGW + OpenSSL headers (C code uses OpenSSL SHA/base64).

## Release process

Do **not** bump versions in build files — release is driven by `.github/release.json`. Editing it on `master` triggers CI (`build_release.yml`): reads `version`/`code`, auto-tags `v<version>`, builds the DLL + APK, and creates a **draft** GitHub release with checksum table and a `segatools.ini` snippet. Everything else (secrets, keystore download) lives in GitHub secrets.

## Licensing

Dual license: `NfcAimeReaderDLL/` (and `aimeio-multi/`, derived from AGPLv3 ppc/AMNet) is AGPLv3-or-later; everything else (incl. the Android app) is WTFPL. Keep license headers in the DLL/C code.
