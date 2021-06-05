package cn.com.lasong.zapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/5
 * Description:
 */
class CoreService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}