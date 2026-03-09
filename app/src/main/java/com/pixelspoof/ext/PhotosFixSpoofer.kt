package com.pixelspoof.ext

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.neonorbit.dexplore.DexFactory
import io.github.neonorbit.dexplore.filter.ClassFilter
import io.github.neonorbit.dexplore.filter.DexFilter
import io.github.neonorbit.dexplore.filter.MethodFilter
import io.github.neonorbit.dexplore.filter.ReferenceTypes
import io.github.neonorbit.dexplore.result.MethodData
import io.github.neonorbit.dexplore.Dexplore

class PhotosFixSpoofer : IXposedHookLoadPackage {

    private val TAG = "PixelSpoofExt"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != Constants.PACKAGE_NAME_GOOGLE_PHOTOS) return

        try {
            XposedBridge.log("$TAG: Hooking Google Photos for DCIM exclusion block...")

            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.builder().addString().build())
                .setReferenceFilter { pool -> pool.stringsContain("/dcim/") }
                .build()

            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.builder().addString().build())
                .setReferenceFilter { pool -> pool.stringsContain("/dcim/") }
                .build()

            val appInfo = lpparam.appInfo
            val apkPaths = mutableListOf<String>()
            appInfo.sourceDir?.let { apkPaths.add(it) }
            appInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

            XposedBridge.log("$TAG: Scanning ${apkPaths.size} APK(s)...")

            var hooked = false
            for (apkPath in apkPaths) {
                try {
                    val dexplore: Dexplore = DexFactory.load(apkPath)
                    val result: MethodData? = dexplore.findMethod(DexFilter.MATCH_ALL, classFilter, methodFilter)
                    if (result != null) {
                        XposedBridge.hookMethod(result.loadMethod(lpparam.classLoader), object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                // Block ALL folders equally - unconditionally return false
                                XposedBridge.log("$TAG: Hook fired, blocking backup call globally.")
                                param.result = false
                            }
                        })
                        XposedBridge.log("$TAG: Successfully hooked method in: $apkPath")
                        hooked = true
                        break
                    } else {
                        XposedBridge.log("$TAG: DCIM exclusion method not found in: $apkPath")
                    }
                } catch (e: Exception) {
                    XposedBridge.log("$TAG: Error scanning $apkPath: ${e.message}")
                }
            }

            if (!hooked) {
                XposedBridge.log("$TAG: WARNING - Could not find target DCIM exclusion method in any APK. Google Photos may have been updated.")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Fatal error initializing hook: ${e.message}")
        }
    }
}
