package com.pixelspoof.ext

import android.os.Build
import android.util.Log
import com.pixelspoof.ext.Constants.PACKAGE_NAME_GOOGLE_PHOTOS
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class DeviceSpoofer: IXposedHookLoadPackage {

    private fun log(message: String){
        XposedBridge.log("PixelifyGooglePhotos: $message")
        Log.d("PixelifyGooglePhotos", message)
    }

    private val pref by lazy {
        XSharedPreferences("com.pixelspoof.ext", Constants.SHARED_PREF_FILE_NAME)
    }

    private val finalDeviceToSpoof: DeviceProps.DeviceEntries? by lazy {
        DeviceProps.getDeviceProps("Pixel XL")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        if (lpparam?.packageName != PACKAGE_NAME_GOOGLE_PHOTOS) return

        log("Loaded DeviceSpoofer for ${lpparam?.packageName}")
        log("Device spoof: ${finalDeviceToSpoof?.deviceName}")

        val propsToSpoof = finalDeviceToSpoof?.props
        if (propsToSpoof == null || propsToSpoof.isEmpty()) return
        val classLoader = lpparam?.classLoader ?: return
        val classBuild = XposedHelpers.findClass("android.os.Build", classLoader)
        for ((key, value) in propsToSpoof) {
            XposedHelpers.setStaticObjectField(classBuild, key, value)
        }

    }
}
