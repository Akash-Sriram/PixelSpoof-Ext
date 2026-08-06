# PixelSpoof-Ext

A lightweight, headless Xposed module for Google Photos that combines unlimited Pixel XL backups with whitelist-free folder separation.

## Features

- **Unlimited Backup**: Spoofs Google Pixel XL (2016) device properties and feature flags to enable lifetime, original-quality backup.
- **Folder Separation**: Separates subfolders inside `/DCIM/` in Google Photos so they are recognized individually.
- **No Upload Whitelist**: Removes the default `/DCIM/Camera/` upload restriction, allowing you to disable backup for your main Camera folder.
- **Headless Design**: Zero launcher icons, UI overhead, or background configurations. Runs automatically upon Google Photos launch.

## Credits

Derived from:
- [samson910022/pixelify-google-photos-modern](https://github.com/samson910022/pixelify-google-photos-modern)
- [RevealedSoulEven/XposedPhotosFIX](https://github.com/RevealedSoulEven/XposedPhotosFIX)
