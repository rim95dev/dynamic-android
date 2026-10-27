# Dynamic Android

An iPhone-style Dynamic Island for Android, built for the Galaxy Z Fold 8.

New notifications grow out of the front camera as a small island, show the app, title and text for a few seconds, and then slip back into the camera. Tap the island to open the notification.

> **Work in progress.** No official APK has been released yet. The app is still changing, so behavior, settings and this guide may change without notice. Until the first release, build it from source.

## Why

There are already a few apps that bring the Dynamic Island to Android, but they are full of ads. So I decided to build my own.

## Requirements

- Android 16 or later
- Tested on the Galaxy Z Fold 8 with One UI 9, on both the cover screen and the main screen

## Setup

### 1. Install the app

No release has been published yet, so for now build from source: open the project in Android Studio and run it, or connect the phone with USB debugging on and run:

```sh
./gradlew installDebug
```

Once releases are available, download the latest `dynamic-android-vX.Y.Z.apk` from [Releases](https://github.com/rim95dev/dynamic-android/releases) on the phone and open it. If the phone asks, allow your browser or file manager to install unknown apps. If a copy built with a different signing key is installed, such as one you built yourself, uninstall it first. Android refuses to update an app whose signature has changed.

### 2. Allow the two permissions

Open **Dynamic Android** and turn on both permissions from its first screen.

- **Accessibility service**: tap **Open settings**, find **Dynamic Island** under installed apps and turn it on. The app uses it only to draw the island above the status bar.
- **Notification access**: tap **Open settings** and turn on **Dynamic Island notifications**. The app uses it to read new notifications.

Apps installed from an APK file usually start with these switches greyed out. If so, open the app info page, tap ⋮ and choose **Allow restricted settings** first.

### 3. Turn off the system pop-ups

The island does not replace your phone's own notification pop-ups. Android does not let an app hide them, so **if you leave them on, every notification shows up twice**: once as the system pop-up and once in the island.

On Samsung phones, turn them off over adb:

```sh
# Turn off the standard pop-ups ("Detailed" style).
adb shell settings put global heads_up_notifications_enabled 0

# Switch away from the "Brief" style (edge lighting), which ignores the setting above.
adb shell settings put system edge_lighting 0
```

You need both. With only the first one, Samsung's brief pop-ups still appear.

To bring the pop-ups back:

```sh
adb shell settings put global heads_up_notifications_enabled 1
adb shell settings put system edge_lighting 1
```

Without adb, you can turn off pop-ups app by app in the phone's settings instead (Apps → the app → Notifications → Notification categories → turn off pop-ups). Menu names differ between One UI versions. Keep sound or vibration on for those apps, because the island skips silent notifications.

Once the pop-ups are off, notifications from apps you hide in the island settings only appear in the notification shade.

### 4. Line up the island with the camera

Open **Camera position → Adjust**. The screen turns white and shows dashed lines that cross at the island's center, with a ring for the island's circle. Change **Horizontal**, **Vertical** and **Circle size** until the lines cross at the center of the camera and the ring sits right around it.

Do this once per screen. On a foldable, do it once on the cover screen and once on the main screen. The position follows the screen when you rotate the phone, so you can line it up in any orientation. **Reset** goes back to the detected position.

## Settings

- **Animation**
  - **Style 1**: the island stretches and drops into a card in one motion.
  - **Style 2**: the island stretches into a pill, pauses, then drops into a card. The pause is adjustable from 0 to 300 ms.
  - **Test** shows a sample island right away, so you can try the style and camera position.
- **Notification apps**
  - **Show all apps**: every app appears in the island except the ones you check.
  - **Show no apps**: only the apps you check appear in the island.

## Rotation

The island always grows out of the camera, wherever rotating the phone puts it:

- **Camera at the top**: the island opens downwards, with the notification below the camera.
- **Camera on the left or right** (landscape, or a foldable's main screen held upright): the island stretches into the screen from the camera and opens up and down around it, with the notification beside the camera.
- **Camera at the bottom** (upside down): the island opens upwards, with the notification above the camera.

## What shows in the island

The island shows new notifications that would normally alert you, while the screen is on, including the lock screen. It skips:

- Ongoing notifications such as music, downloads, calls and alarms
- Group summaries
- Silent notifications and notifications blocked by Do Not Disturb
- Quiet updates to a notification that is already showing

On the lock screen, the island shows only the app name and "New notification" unless the notification is marked as public. Notifications marked as secret are not shown there at all.

## Development

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK
```

Android Studio's bundled JDK works for command-line builds:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

## Releasing

Pushing a tag such as `v0.1.0` runs the [release workflow](.github/workflows/release.yml). It runs the unit tests, builds a release APK shrunk with R8, signs it and attaches it to a GitHub release for that tag. The version name comes from the tag.

The signing key never goes into the repository. Set it up once:

1. Create a keystore on a machine you trust. `keytool` asks for a password; choose your own.

   ```sh
   keytool -genkeypair -v -keystore dynamic-android-release.jks -alias dynamic-android \
     -keyalg RSA -keysize 4096 -validity 36500
   ```

   Back up the keystore file and its password somewhere safe, such as a password manager. If you lose them, installed copies can no longer be updated and users have to uninstall first.

2. In the repository on GitHub, open **Settings → Secrets and variables → Actions** and add these repository secrets:

   | Secret | Value |
   | --- | --- |
   | `RELEASE_KEYSTORE_BASE64` | Output of `base64 -i dynamic-android-release.jks` |
   | `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
   | `RELEASE_KEY_ALIAS` | `dynamic-android` |
   | `RELEASE_KEY_PASSWORD` | The key password. `keytool` uses the keystore password for it by default. |

3. Tag a commit and push the tag:

   ```sh
   git tag v0.1.0
   git push origin v0.1.0
   ```

The workflow stops with an error if any of the secrets is missing, so an unsigned APK is never published.
