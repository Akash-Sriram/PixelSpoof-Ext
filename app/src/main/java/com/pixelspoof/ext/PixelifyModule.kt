package com.pixelspoof.ext

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class PixelifyModule : XposedModule() {

    companion object {
        const val TAG = "PixelSpoof"
    }

    override fun onModuleLoaded(params: XposedModuleInterface.ModuleLoadedParam) {
        Log.d(TAG, "PixelSpoof-Ext module loaded (Headless Mode)")
    }

    override fun onPackageLoaded(params: XposedModuleInterface.PackageLoadedParam) {
        if (params.packageName != Constants.PACKAGE_NAME_GOOGLE_PHOTOS) return
        if (!params.isFirstPackage) {
            Log.v(TAG, "Skipping non-first package load for ${params.packageName}")
            return
        }

        Log.d(TAG, "Google Photos package loaded. Early device spoof...")
        try {
            DeviceSpoofer.hook(this, allowFailureUi = false)
            Log.d(TAG, "DeviceSpoofer early apply done")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed early DeviceSpoofer apply", t)
        }
    }

    override fun onPackageReady(params: XposedModuleInterface.PackageReadyParam) {
        when (params.packageName) {
            Constants.PACKAGE_NAME_GOOGLE_PHOTOS -> {
                Log.d(TAG, "Google Photos ready (${params.packageName}). Applying hooks...")
                try {
                    try {
                        FeatureSpoofer.hook(this, params.classLoader)
                        Log.d(TAG, "FeatureSpoofer hook registered")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to register FeatureSpoofer hooks", t)
                    }

                    try {
                        DeviceSpoofer.hook(this)
                        Log.d(TAG, "DeviceSpoofer hook registered")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to register DeviceSpoofer hooks", t)
                    }

                    // Apply DCIM folder backup separation fix (XposedPhotosFIX)
                    try {
                        val atClass = params.classLoader.loadClass("android.app.ActivityThread")
                        val currentActivityThread = atClass.getMethod("currentActivityThread").invoke(null)
                        val getSystemContext = atClass.getMethod("getSystemContext")
                        getSystemContext.isAccessible = true
                        val systemContext = getSystemContext.invoke(currentActivityThread) as? android.content.Context
                        if (systemContext != null) {
                            val pm = systemContext.packageManager
                            val appInfo = pm.getApplicationInfo(Constants.PACKAGE_NAME_GOOGLE_PHOTOS, 0)
                            val packageInfo = pm.getPackageInfo(Constants.PACKAGE_NAME_GOOGLE_PHOTOS, 0)
                            val appVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode.toLong()
                            }
                            
                            val cache = CacheManager.loadCache()
                            if (cache != null && cache.appVersion == appVersion && cache.moduleVersion == CacheManager.MODULE_VERSION) {
                                Log.d(TAG, "Valid operational cache discovered. Resolving intercept definitions...")
                                if (cache.builderClassName != null && cache.setterMethodName != null) {
                                    CacheManager.executeHook(cache.builderClassName, cache.setterMethodName, cache.filepathFieldName, this, params.classLoader)
                                }
                                if (cache.legacyClassName != null && cache.legacyMethodName != null) {
                                    CacheManager.executeLegacyHook(cache.legacyClassName, cache.legacyMethodName, this, params.classLoader)
                                }
                            } else {
                                Log.d(TAG, "Cache missing/mismatched. Launching dynamic bytecode extraction engine...")
                                CacheManager.scanAndHook(this, appInfo, params.classLoader, appVersion)
                            }
                        } else {
                            Log.w(TAG, "Could not retrieve systemContext for DCIM fix")
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to register DCIM backup separation fix hooks", t)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to register hooks", t)
                }
            }
        }
    }
}
