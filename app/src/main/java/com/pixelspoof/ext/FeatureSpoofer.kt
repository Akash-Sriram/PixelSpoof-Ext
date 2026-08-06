package com.pixelspoof.ext

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

object FeatureSpoofer {

    private const val TAG = "Pixelify"
    private const val CLASS_APPLICATION_MANAGER = "android.app.ApplicationPackageManager"

    private val finalFeaturesToSpoof = setOf(
        "com.google.android.apps.photos.NEXUS_PRELOAD",
        "com.google.android.apps.photos.nexus_preload",
        "com.google.android.feature.PIXEL_EXPERIENCE",
        "com.google.android.apps.photos.PIXEL_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2016_PRELOAD"
    )

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = classLoader.loadClass(CLASS_APPLICATION_MANAGER)

            val methodString = clazz.getDeclaredMethod("hasSystemFeature", String::class.java)
            module.hook(methodString).intercept { chain -> decideSpoof(chain) }

            val methodStringInt = clazz.getDeclaredMethod("hasSystemFeature", String::class.java, Int::class.javaPrimitiveType)
            module.hook(methodStringInt).intercept { chain -> decideSpoof(chain) }

            Log.d(TAG, "FeatureSpoofer hooks registered successfully (Pixel XL hardcoded)")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register FeatureSpoofer hooks", t)
        }
    }

    private fun decideSpoof(chain: XposedInterface.Chain): Any? {
        val feature = chain.getArg(0) as? String ?: return chain.proceed()
        return if (feature in finalFeaturesToSpoof) {
            true
        } else {
            chain.proceed()
        }
    }
}
