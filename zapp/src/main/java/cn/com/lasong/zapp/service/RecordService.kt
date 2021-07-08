package cn.com.lasong.zapp.service

import android.media.projection.MediaProjection
import android.os.Message
import cn.com.lasong.zapp.data.RecordBean

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/8
 * Description:
 * 录制服务
 */
class RecordService : CoreService() {

    companion object {
        const val MSG_SCREEN_RECORD = 1
    }

    // 是否正在录制
    var isRecording = false

    // 录制屏幕对象
    lateinit var mediaProjection: MediaProjection

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            MSG_UNREGISTER_CLIENT -> {
                stopRecord()
            }
        }
        return super.handleMessage(msg)
    }

    fun startRecord(params : RecordBean) {
//        startForegroundService()
    }

    fun stopRecord() {

    }

}