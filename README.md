# PixelSpoof-Ext

A lightweight, headless Xposed module for Google Photos that combines unlimited Pixel XL backups with whitelist-free folder separation.

## Features

- **Unlimited Backup**: Spoofs Google Pixel XL (2016) device properties and feature flags to enable lifetime, original-quality backup.
- **Folder Separation**: Separates subfolders inside `/DCIM/` in Google Photos so they are recognized individually.
- **No Upload Whitelist**: Removes the default `/DCIM/Camera/` upload restriction, allowing you to disable backup for your main Camera folder.
- **Headless Design**: Zero launcher icons, UI overhead, or background configurations. Runs automatically upon Google Photos launch.

## Requirements

- Android 8.0 to Android 15/16
- Root with a modern Xposed environment supporting `libxposed` API (e.g., LSPosed)
- Google Photos (`com.google.android.apps.photos`)

## Installation

1. Download the APK from the [Releases](https://github.com/Akash-Sriram/PixelSpoof-Ext/releases) page.
2. Install the APK and enable **PixelSpoof-Ext** in your Xposed manager (scoped to **Google Photos** only).
3. Clear Google Photos app data (**Settings > Apps > Google Photos > Clear Storage/Data**).
4. Re-open Google Photos. Unlimited backup will be active, and the Camera folder backup toggle will be accessible under **Backup > Back up device folders**.

## Credits

Derived from:
- [samson910022/pixelify-google-photos-modern](https://github.com/samson910022/pixelify-google-photos-modern)
- [RevealedSoulEven/XposedPhotosFIX](https://github.com/RevealedSoulEven/XposedPhotosFIX)
