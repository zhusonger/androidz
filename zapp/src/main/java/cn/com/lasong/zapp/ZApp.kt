package cn.com.lasong.zapp

import android.content.Context
import android.util.Log
import cn.com.lasong.base.BaseApplication
import cn.com.lasong.utils.ILog
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/7
 * Description:
 */
class ZApp : BaseApplication() {

    init { INSTANCE = this }

    companion object {
        lateinit var INSTANCE: ZApp
            private set

        const val KEY_RECORD_SAVE: String = "record_save"
        val JSON = Gson()
        fun applicationContext() : Context {
            return INSTANCE.applicationContext
        }
        fun appInstance() : ZApp {
            return INSTANCE
        }
    }
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            ILog.setLogLevel(Log.DEBUG)
        }
        val rootDir: String = MMKV.initialize(this)
        ILog.d("mmkv root: $rootDir")
    }
}