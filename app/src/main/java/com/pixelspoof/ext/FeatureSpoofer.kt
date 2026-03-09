package com.pixelspoof.ext

import android.util.Log
import com.pixelspoof.ext.Constants.PACKAGE_NAME_GOOGLE_PHOTOS
import com.pixelspoof.ext.Constants.SHARED_PREF_FILE_NAME
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FeatureSpoofer: IXposedHookLoadPackage {

    /**
     * Actual class is not android.content.pm.PackageManager.
     * It is an abstract class which cannot be hooked.
     * Actual class found from stackoverflow:
     * https://stackoverflow.com/questions/66523720/xposed-cant-hook-getinstalledapplications
     */
    private val CLASS_APPLICATION_MANAGER = "android.app.ApplicationPackageManager"

    /**
     * Method hasSystemFeature(). Two signatures exist. We need to hook both.
     * https://developer.android.com/reference/android/content/pm/PackageManager#hasSystemFeature(java.lang.String)
     * https://developer.android.com/reference/android/content/pm/PackageManager#hasSystemFeature(java.lang.String,%20int)
     */
    private val METHOD_HAS_SYSTEM_FEATURE = "hasSystemFeature"

    /**
     * Simple message to log messages in lsposed log as well as android log.
     */
    private fun log(message: String){
        XposedBridge.log("PixelSpoofExt: $message")
        Log.d("PixelSpoofExt", message)
    }

    /**
     * To read preference of user.
     */
    private val pref by lazy {
        XSharedPreferences("com.pixelspoof.ext", SHARED_PREF_FILE_NAME).apply {
            log("Preference location: ${getFile().canonicalPath}")
        }
    }

    private val finalFeaturesToSpoof: List<String> by lazy {
        val allFeatureFlags = ArrayList<String>(0)
        DeviceProps.defaultFeatures.forEach {
            allFeatureFlags.addAll(it.featureFlags)
        }
        allFeatureFlags
    }

    /**
     * If a feature needed for google photos is needed, i.e. features in [finalFeaturesToSpoof],
     * then set result of hooked method [METHOD_HAS_SYSTEM_FEATURE] as `true`.
     */
    private fun spoofFeatureEnquiryResultIfNeeded(param: XC_MethodHook.MethodHookParam?){
        val arguments = param?.args?.toList()

        var passFeatureTrue = false

        arguments?.forEach {
            if (it.toString() in finalFeaturesToSpoof) passFeatureTrue = true
        }

        if (passFeatureTrue) param?.setResult(true)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        if (lpparam?.packageName != PACKAGE_NAME_GOOGLE_PHOTOS) return

        log("Loaded FeatureSpoofer for ${lpparam?.packageName}")

        /**
         * Hook hasSystemFeature(String).
         */
        XposedHelpers.findAndHookMethod(
            CLASS_APPLICATION_MANAGER,
            lpparam?.classLoader,
            METHOD_HAS_SYSTEM_FEATURE, String::class.java,
            object: XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    spoofFeatureEnquiryResultIfNeeded(param)
                }

            }
        )

        /**
         * Hook hasSystemFeature(String, int).
         */
        XposedHelpers.findAndHookMethod(
            CLASS_APPLICATION_MANAGER,
            lpparam?.classLoader,
            METHOD_HAS_SYSTEM_FEATURE, String::class.java, Int::class.java,
            object: XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    spoofFeatureEnquiryResultIfNeeded(param)
                }

            }
        )

    }
}
