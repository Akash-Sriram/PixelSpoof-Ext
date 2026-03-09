# PixelSpoof Ext (v1.0)

**PixelSpoof Ext** is a specialized Xposed module that combines the best features of two powerful projects to provide a premium Google Photos experience on non-Pixel devices.

This project is a combo of:
*   [**Pixelify-Google-Photos**](https://github.com/BaltiApps/Pixelify-Google-Photos): Providing core device identity spoofing (Pixel XL).
*   [**XposedPhotosFIX**](https://github.com/RevealedSoulEven/XposedPhotosFIX): Providing the critical DCIM backup control logic.

## 🚀 Key Features

*   **Pixel XL Identity Spoofing**: Spoofs your device as a **Google Pixel XL (2016)**, which is the only profile that still offers **unlimited "Original Quality" backups** in Google Photos.
*   **DCIM Backup Control**: Removes the "privileged" status of the DCIM directory, allowing you to explicitly prevent Google Photos from forcing backups of your camera folder.
*   **Stealth Operation**: Disguised as a system service (`com.pixelspoof.ext`) to avoid detection and maintain a clean system environment.
*   **Dynamic Scanning**: Uses `dexplore` to dynamically locate and hook methods, ensuring resilience against Google Photos app updates.

## 🛠 Credits & Inspiration

This module would not be possible without the excellent work of the original developers:
- [BaltiApps](https://github.com/BaltiApps) for the foundational Pixelify logic.
- [RevealedSoulEven](https://github.com/RevealedSoulEven) for the DCIM backup blocking fix.

## 📦 Installation

1.  Root your device and install an Xposed framework (e.g., LSPosed).
2.  Download the `app-release.apk` (v1.0) from the [Releases](https://github.com/Akash-Sriram/PixelSpoof-Ext/releases) area.
3.  Install the APK and enable it in your Xposed manager.
4.  Ensure **Google Photos** is in the module's scope.
5.  Restart Google Photos or reboot your device.

---
*Maintained by [Akash Sriram](https://github.com/Akash-Sriram).*
