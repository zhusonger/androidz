package cn.com.lasong.zapp.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
        const val MSG_QUERY_RECORD = 0
        const val MSG_RECORD_START = 1

        const val KEY_RECORDING = "recording"
        const val KEY_MEDIA_PROJECTION_NULL = "mediaProjection_null"
        const val KEY_RECORD_PARAMS = "record_params"
    }

    // 是否正在录制
    var isRecording = false

    // 录制屏幕对象
    private var mediaProjection: MediaProjection? = null

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            MSG_QUERY_RECORD -> {
                // 返回正在录制的查询结果
                val message = Message.obtain(handler, MSG_QUERY_RECORD)
                message.obj = mapOf(KEY_RECORDING to isRecording, KEY_MEDIA_PROJECTION_NULL to (mediaProjection == null))
                sendMessage(message)
            }

            MSG_RECORD_START -> {
                isRecording = true
                // 开始录制
                if (null == mediaProjection) {
                    val resultData = msg.obj as Intent
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = manager.getMediaProjection(Activity.RESULT_OK, resultData)
                }

                val message = Message.obtain(handler, MSG_RECORD_START)
                message.obj = RES_OK
                sendMessage(message)
            }

            MSG_UNREGISTER_CLIENT -> {
                stopRecord()
            }
        }
        return super.handleMessage(msg)
    }

    fun startRecord(params : RecordBean) {

    }

    fun stopRecord() {

    }

}