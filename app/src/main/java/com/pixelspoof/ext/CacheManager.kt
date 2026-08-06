package com.pixelspoof.ext

import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedModule
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.query.matchers.MethodsMatcher
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object CacheManager {

    private const val TAG = "Pixelify"
    private const val CACHE_PATH = "/data/data/com.google.android.apps.photos/files/photosmod.cache"
    const val MODULE_VERSION = 4

    class Cache(
        val appVersion: Long,
        val moduleVersion: Int,
        val builderClassName: String?,
        val setterMethodName: String?,
        val filepathFieldName: String?,
        val legacyClassName: String?,
        val legacyMethodName: String?
    )



    fun loadCache(): Cache? {
        return try {
            val file = File(CACHE_PATH)
            if (!file.exists()) return null

            val content = String(Files.readAllBytes(Paths.get(CACHE_PATH)), StandardCharsets.UTF_8)
            val json = JSONObject(content)

            Cache(
                json.getLong("appVersion"),
                json.getInt("moduleVersion"),
                json.optString("builderClassName", null),
                json.optString("setterMethodName", null),
                json.optString("filepathFieldName", null),
                json.optString("legacyClassName", null),
                json.optString("legacyMethodName", null)
            )
        } catch (t: Throwable) {
            null
        }
    }

    fun saveCache(cache: Cache) {
        try {
            val file = File(CACHE_PATH)
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }

            val json = JSONObject()
            json.put("appVersion", cache.appVersion)
            json.put("moduleVersion", cache.moduleVersion)
            if (cache.builderClassName != null) {
                json.put("builderClassName", cache.builderClassName)
                json.put("setterMethodName", cache.setterMethodName)
                json.put("filepathFieldName", cache.filepathFieldName)
            }
            if (cache.legacyClassName != null) {
                json.put("legacyClassName", cache.legacyClassName)
                json.put("legacyMethodName", cache.legacyMethodName)
            }

            Files.write(Paths.get(CACHE_PATH), json.toString().toByteArray(StandardCharsets.UTF_8))
            Log.d(TAG, "Dynamic configurations saved to internal file cache.")
        } catch (ignored: Throwable) {}
    }

    private var nativeReady = false

    fun ensureNativeLoaded(module: XposedModule) {
        if (nativeReady) return
        val appInfo = try {
            module.moduleApplicationInfo
        } catch (t: Throwable) {
            Log.w(TAG, "getModuleApplicationInfo failed: ${t.message}")
            null
        } ?: return

        val hostApp = try {
            val at = Class.forName("android.app.ActivityThread")
            val currentActivityThread = at.getMethod("currentActivityThread").invoke(null)
            at.getMethod("currentApplication").invoke(currentActivityThread) as? android.app.Application
        } catch (_: Throwable) {
            null
        }

        val candidates = mutableListOf<File>()
        val abis = android.os.Build.SUPPORTED_ABIS?.toList().orEmpty().ifEmpty {
            listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        val outDir = File("/data/data/com.google.android.apps.photos/code_cache/dexkit_native")
        if (!outDir.exists()) outDir.mkdirs()

        val apkPath = appInfo.sourceDir ?: return
        try {
            java.util.zip.ZipFile(apkPath).use { zip ->
                for (abi in abis) {
                    val entryName = "lib/$abi/libdexkit.so"
                    val entry = zip.getEntry(entryName) ?: continue
                    val out = File(outDir, "libdexkit-$abi.so")
                    if (!out.isFile || out.length() != entry.size) {
                        zip.getInputStream(entry).use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                        out.setReadable(true, false)
                        out.setExecutable(true, false)
                    }
                    candidates += out
                    break
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "APK native extract failed for dexkit: ${t.message}")
        }

        var loaded = false
        for (so in candidates) {
            try {
                System.load(so.absolutePath)
                nativeReady = true
                loaded = true
                Log.d(TAG, "Loaded libdexkit from ${so.absolutePath}")
                break
            } catch (t: Throwable) {
                Log.w(TAG, "System.load(${so.absolutePath}) failed: ${t.message}")
            }
        }

        if (!loaded) {
            try {
                System.loadLibrary("dexkit")
                nativeReady = true
                Log.d(TAG, "Loaded libdexkit via System.loadLibrary fallback")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load dexkit native library", t)
            }
        }
    }

    fun scanAndHook(module: XposedModule, appInfo: android.content.pm.ApplicationInfo, classLoader: ClassLoader, appVersion: Long) {
        ensureNativeLoaded(module)
        if (!nativeReady) {
            Log.e(TAG, "dexkit native library not loaded; aborting bytecode scan")
            return
        }

        var builderClassName: String? = null
        var setterMethodName: String? = null
        var filepathFieldName: String? = null
        var legacyClassName: String? = null
        var legacyMethodName: String? = null

        val apkPaths = mutableListOf<String>()
        appInfo.sourceDir?.let { apkPaths.add(it) }
        appInfo.splitSourceDirs?.forEach { apkPaths.add(it) }

        for (apkPath in apkPaths) {
            if (builderClassName != null && setterMethodName != null) {
                break
            }
            try {
                DexKitBridge.create(apkPath).use { bridge ->
                    if (bridge != null) {
                        val inserterClasses = bridge.findClass(
                            FindClass.create()
                                .matcher(
                                    ClassMatcher.create()
                                        .usingStrings("/dcim/", "LocalMediaInsert")
                                )
                        )

                        if (!inserterClasses.isEmpty()) {
                            val inserterClassName = inserterClasses[0].name
                            Log.d(TAG, "Discovered inserter class name: $inserterClassName")

                            val setterMethods = bridge.findMethod(
                                FindMethod.create()
                                    .matcher(
                                        MethodMatcher.create()
                                            .returnType("void")
                                            .paramCount(1)
                                            .paramTypes("boolean")
                                            .usingNumbers(32) // isolates inCameraFolder from isHidden (16) and raw (8)
                                            .declaredClass(
                                                ClassMatcher.create()
                                                    .usingStrings("Missing required properties:", " inCameraFolder")
                                            )
                                            .callerMethods(
                                                MethodsMatcher.create()
                                                    .add(MethodMatcher.create().declaredClass(ClassMatcher.create().className(inserterClassName)))
                                            )
                                    )
                            )

                            if (!setterMethods.isEmpty()) {
                                val targetMethod = setterMethods[0]
                                builderClassName = targetMethod.className
                                setterMethodName = targetMethod.name
                                Log.d(TAG, "Discovered builder class: $builderClassName, method: $setterMethodName")

                                val fields = bridge.findField(
                                    FindField.create()
                                        .matcher(
                                            FieldMatcher.create()
                                                .declaredClass(ClassMatcher.create().className(builderClassName))
                                                .writeMethods(
                                                    MethodsMatcher.create()
                                                        .add(MethodMatcher.create().usingStrings("Null filepath"))
                                                )
                                        )
                                )

                                if (!fields.isEmpty()) {
                                    filepathFieldName = fields[0].name
                                    Log.d(TAG, "Discovered internal path field mapping: $filepathFieldName")
                                }
                            }
                        }

                        if (legacyClassName == null) {
                            val legacyMethods = bridge.findMethod(
                                FindMethod.create()
                                    .matcher(
                                        MethodMatcher.create()
                                            .returnType("boolean")
                                            .usingStrings("/dcim/")
                                    )
                            )

                            if (!legacyMethods.isEmpty()) {
                                val legacyMethod = legacyMethods[0]
                                legacyClassName = legacyMethod.className
                                legacyMethodName = legacyMethod.name
                                Log.d(TAG, "Discovered legacy boolean method: $legacyClassName.$legacyMethodName")
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "DexKit engine processing anomaly for $apkPath: ${t.message}")
            }
        }

        val hasStructural = builderClassName != null && setterMethodName != null
        val hasLegacy = legacyClassName != null && legacyMethodName != null

        if (!hasStructural && !hasLegacy) {
            Log.e(TAG, "Failure: Neither structural nor legacy methods were found. Module cannot operate on this build.")
            return
        }

        if (hasStructural) {
            executeHook(builderClassName!!, setterMethodName!!, filepathFieldName, module, classLoader)
        }
        if (hasLegacy) {
            executeLegacyHook(legacyClassName!!, legacyMethodName!!, module, classLoader)
        }

        val newCache = Cache(
            appVersion,
            MODULE_VERSION,
            builderClassName,
            setterMethodName,
            filepathFieldName,
            legacyClassName,
            legacyMethodName
        )
        saveCache(newCache)
    }

    fun executeLegacyHook(className: String, methodName: String, module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = classLoader.loadClass(className)
            for (method in clazz.declaredMethods) {
                if (method.name == methodName) {
                    module.hook(method).intercept { chain ->
                        val filepath = chain.args.firstOrNull() as? String
                        // Remove /dcim/camera/ whitelist completely!
                        if (filepath != null && filepath.contains("/dcim/", ignoreCase = true)) {
                            false
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
            Log.d(TAG, "Legacy intercept runtime hooks mounted securely.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to instantiate legacy mapping layers: ${t.message}")
        }
    }

    fun executeHook(className: String, methodName: String, fieldName: String?, module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = classLoader.loadClass(className)
            val method = clazz.getDeclaredMethod(methodName, Boolean::class.javaPrimitiveType)
            module.hook(method).intercept { chain ->
                val inCameraFolder = chain.args[0] as? Boolean
                if (inCameraFolder == true) {
                    var filepath: String? = null
                    val thisObject = chain.thisObject
                    
                    if (fieldName != null) {
                        try {
                            val field = thisObject.javaClass.getDeclaredField(fieldName)
                            field.isAccessible = true
                            val optionalPath = field.get(thisObject)
                            if (optionalPath != null) {
                                val isPresent = optionalPath.javaClass.getMethod("isPresent").invoke(optionalPath) as? Boolean
                                if (isPresent == true) {
                                    val pathObj = optionalPath.javaClass.getMethod("get").invoke(optionalPath)
                                    filepath = pathObj?.toString()
                                }
                            }
                        } catch (ignored: Throwable) {}
                    }
                    
                    if (filepath == null) {
                        try {
                            val fields = thisObject.javaClass.getDeclaredFields()
                            for (field in fields) {
                                if (field.type.name.contains("Optional")) {
                                    field.isAccessible = true
                                    val optionalObj = field.get(thisObject)
                                    if (optionalObj != null) {
                                        val isPresent = optionalObj.javaClass.getMethod("isPresent").invoke(optionalObj) as? Boolean
                                        if (isPresent == true) {
                                            val valObj = optionalObj.javaClass.getMethod("get").invoke(optionalObj)
                                            if (valObj is String) {
                                                if (valObj.contains("/dcim/", ignoreCase = true)) {
                                                    filepath = valObj
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            Log.e(TAG, "Dynamic evaluation scanner anomaly: ${t.message}")
                        }
                    }
                    
                    // Remove /dcim/camera/ whitelist completely!
                    if (filepath != null && filepath.contains("/dcim/", ignoreCase = true)) {
                        chain.args[0] = false
                    }
                }
                chain.proceed()
            }
            Log.d(TAG, "Structural intercept runtime hooks mounted securely.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to instantiate operations mapping layers: ${t.message}")
        }
    }
}
