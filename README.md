# PixelSpoof Ext

**PixelSpoof Ext** is a specialized Xposed module designed to unlock **Unlimited Original Quality storage** in Google Photos by spoofing your device as a **Google Pixel XL**. It provides a lightweight, stealthy, and effective way to bypass storage restrictions and manage backups on non-Pixel Android devices.

## 🚀 Key Features

*   **Pixel XL Identity Spoofing**: Intercepts system calls to present your device as a genuine Google Pixel XL, which is specifically recognized by Google Photos for permanent, unlimited "Original Quality" backups.
*   **DCIM Backup Control**: Removes the "privileged" status of the DCIM (Camera) folder. This allows you to prevent Google Photos from forcing backups of your primary gallery, giving you total control over what gets uploaded.
*   **Stealth & Integration**: Operates under a discrete system-service identity (`com.pixelspoof.ext`) to avoid detection and blend seamlessly into the Android OS.
*   **Dynamic Compatibility**: Employs advanced code-scanning (`dexplore`) to dynamically locate and hook the necessary methods within Google Photos, ensuring the module remains functional across app updates.

## 🛠 Project Purpose

While many "Pixelify" tools attempt general spoofing, **PixelSpoof Ext** focuses specifically on the **Pixel XL (2016)** profile. This is the only device profile that still offers unlimited backup in **Original Quality**, making this module a powerful tool for photographers and power users.

## 📦 Installation

1.  Root your device and install a modern Xposed framework (e.g., LSPosed).
2.  Download the `app-release.apk` from the [Releases](https://github.com/Akash-Sriram/PixelSpoof-Ext/releases) area.
3.  Install the APK.
4.  Enable **PixelSpoof Ext** in your Xposed manager and ensure **Google Photos** is selected in the scope.
5.  Force stop Google Photos or reboot your device to apply the changes.

---
*Created and maintained by [Akash Sriram](https://github.com/Akash-Sriram).*
