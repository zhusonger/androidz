package cn.com.lasong.zapp

import android.util.Log
import cn.com.lasong.base.BaseApplication
import cn.com.lasong.utils.ILog

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/7
 * Description:
 */
class ZApp : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            ILog.setLogLevel(Log.DEBUG)
        }
    }
}