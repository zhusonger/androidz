package cn.com.lasong.zapp

import android.content.Context
import android.util.Log
import cn.com.lasong.base.BaseApplication
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.database.ZAppDatabase
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/7
 * Description:
 */
class ZApp : BaseApplication() {

    // 数据库
    val database by lazy { ZAppDatabase.getDatabase() }

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
            SoLoader.init(this, false);
            if (FlipperUtils.shouldEnableFlipper(this)) {
                val client = AndroidFlipperClient.getInstance(this)
                client.addPlugin(DatabasesFlipperPlugin(this))
                client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
                client.start()
            }
        }
        val rootDir: String = MMKV.initialize(this)
        ILog.d("mmkv root: $rootDir")
    }
}