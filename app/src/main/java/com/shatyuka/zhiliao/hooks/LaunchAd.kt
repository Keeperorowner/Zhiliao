package com.shatyuka.zhiliao.hooks

import com.shatyuka.zhiliao.Helper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement.DO_NOTHING
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import de.robv.android.xposed.XposedBridge.hookMethod
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import java.lang.reflect.Method

class LaunchAd : BaseHook() {

    private var chooseAdUrl: Method? = null
    private var isShowLaunchAd: Method? = null
    private var resolveAdvert: Method? = null

    private var adClassList: MutableList<Class<*>> = ArrayList()
    private var adClassNameList = listOf<String>(
        // https://api.zhihu.com/commercial_api/app_float_layer
        "com.zhihu.android.adbase.model.AdFeedFloat",
        // https://api.zhihu.com/fringe/ad
        "com.zhihu.android.adbase.model.PushAdModel"
    )

    override fun getName(): String {
        return "去启动页广告"
    }

    @Throws(Throwable::class)
    override fun init(classLoader: ClassLoader) {
        chooseAdUrl = findChooseAdUrlMethod(classLoader)
        isShowLaunchAd = findIsShowLaunchAdMethod(classLoader)
        resolveAdvert = findResolveAdvertMethod(classLoader)

        adClassNameList.forEach {
            try {
                adClassList.add(classLoader.loadClass(it))
            } catch (e: Exception) {
                logE(e.message)
            }
        }
    }

    override fun hook() {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)
            || !Helper.prefs.getBoolean("switch_launchad", true)
        ) {
            return
        }

        if (isShowLaunchAd != null) {
            hookMethod(isShowLaunchAd, returnConstant(false))
        }

        if (chooseAdUrl != null) {
            hookMethod(chooseAdUrl, returnConstant(""))
        }

        // 首页开屏浮动广告
        if (resolveAdvert != null) {
            hookMethod(resolveAdvert, DO_NOTHING)
        }

        hookMethod(Helper.JacksonHelper.ObjectReader_readValue, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.result == null) {
                    return
                }
                adClassList.forEach {
                    if (param.result.javaClass == it) {
                        param.result = null
                        return@forEach
                    }
                }
            }
        })
    }

    private fun findChooseAdUrlMethod(classLoader: ClassLoader): Method? {
        val classMatcher: ClassMatcher = ClassMatcher.create().fields {
            add {
                type = "okhttp3.OkHttpClient.Builder"
                type = "boolean"
            }
        }.methods {
            add {
                paramCount = 4
                paramTypes(
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                    String::class.java
                )
            }
            add {
                paramCount = 5
                paramTypes(
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                    String::class.java,
                    null
                )
            }
        }
        val adNetworkManager = Helper.findClass(
            listOf("com.zhihu.android.sdk.launchad"),
            listOf(
                "com.zhihu.android.sdk.launchad.job",
                "com.zhihu.android.sdk.launchad.room",
                "com.zhihu.android.sdk.launchad.utils",
            ),
            classMatcher,
            classLoader
        )
        if (adNetworkManager == null) {
            logE("no class: com.zhihu.android.sdk.launchad.AdNetworkManager")
            return null
        }
        val chooseUrl = Helper.getMethodByParameterTypes(
            adNetworkManager, Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            String::class.java
        )
        if (chooseUrl == null) {
            logE("no method: com.zhihu.android.sdk.launchad.AdNetworkManager#chooseUrl")
        }
        return chooseUrl
    }

    private fun findResolveAdvertMethod(classLoader: ClassLoader): Method? {
        val methodMatcher = MethodMatcher.create()
            .paramCount(5)
            .paramTypes(
                listOf(
                    "com.zhihu.android.adbase.model.Advert",
                    null,
                    "java.lang.String",
                    "android.content.Context",
                    "java.lang.String"
                )
            )

        val methodList = Helper.findMethodList(
            listOf("com.zhihu.android.ad.special"),
            null,
            methodMatcher,
            classLoader
        )
        if (methodList.isEmpty() || methodList.size > 1) {
            logE("no AdSpecialDataProvider#resolveAdvert or multi")
            return null
        }
        return methodList[0]
    }

    private fun findIsShowLaunchAdMethod(classLoader: ClassLoader): Method? {
        val classMatcher: ClassMatcher = ClassMatcher.create()
            .addInterface {
                className = "com.zhihu.android.ad.LaunchAdInterface"
            }
            .methods {
                add {
                    paramCount = 0
                    name = "isShowLaunchAd"
                }
            }

        val launchAdHelper = Helper.findClass(
            listOf("com.zhihu.android.app.util"),
            listOf(
                "com.zhihu.android.app.util.anim",
                "com.zhihu.android.app.util.largetool",
                "com.zhihu.android.app.util.mediaconfig",
                "com.zhihu.android.app.util.netplugable",
                "com.zhihu.android.app.util.oaid",
                "com.zhihu.android.app.util.web"
            ),
            classMatcher,
            classLoader
        )
        if (launchAdHelper == null) {
            logE("no class LaunchAdHelper or multi")
            return null
        }
        return try {
            launchAdHelper.getDeclaredMethod("isShowLaunchAd")
        } catch (e: Exception) {
            logE(e)
            null
        }
    }

}
