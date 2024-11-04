package com.shatyuka.zhiliao.hooks

import com.shatyuka.zhiliao.Helper
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import de.robv.android.xposed.XposedBridge.hookAllMethods

class VIPBanner : BaseHook() {

    private var moreVipDataList: MutableList<Class<*>> = ArrayList()

    private var moreVipDataClassNameList = listOf<String>(
        "com.zhihu.android.api.MoreVipData",
        "com.zhihu.android.profile.data.model.MoreVipData",
        "com.zhihu.android.app.ui.fragment.more.mine.model.MoreKVipData"
    )

    override fun getName(): String {
        return "隐藏会员卡片"
    }

    override fun init(classLoader: ClassLoader) {
        moreVipDataClassNameList.forEach {
            try {
                moreVipDataList.add(classLoader.loadClass(it))
            } catch (e: ClassNotFoundException) {
                logE(e.message)
            }
        }
    }

    override fun hook() {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)
            || !Helper.prefs.getBoolean("switch_vipbanner", false)
        ) {
            return
        }
        moreVipDataList.forEach {
            hookAllMethods(it, "isLegal", returnConstant(false))
        }
    }
}
