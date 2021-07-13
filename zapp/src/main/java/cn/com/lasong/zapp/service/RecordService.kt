package cn.com.lasong.zapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import cn.com.lasong.zapp.MainActivity
import cn.com.lasong.zapp.R
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
        const val MSG_PRE_RECORD = 2

        const val KEY_RECORDING = "recording"
        const val KEY_MEDIA_PROJECTION_NULL = "mediaProjection_null"
        const val KEY_RECORD_PARAMS = "record_params"
        const val KEY_RECORD_START_TIME = "record_start_time"

        const val CHANNEL_ID = "RECORD_VIDEO_CHANNEL_ID"
    }

    // 是否正在录制
    private var isRecording = false

    private var params: RecordBean? = null

    // 录制屏幕对象
    private var mediaProjection: MediaProjection? = null

    // 录制的启动时间戳
    private var elapsedStartTimeMs: Long = 0

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            // 1. 返回正在录制的查询结果
            MSG_QUERY_RECORD, MSG_PRE_RECORD -> {
                val message = Message.obtain(handler, msg.what)
                message.obj = mapOf(KEY_RECORDING to isRecording,
                    KEY_MEDIA_PROJECTION_NULL to (mediaProjection == null),
                    KEY_RECORD_PARAMS to params,
                    KEY_RECORD_START_TIME to elapsedStartTimeMs)
                sendMessage(message)
            }
            // 2. 开始录制
            MSG_RECORD_START -> {
                startForeground()
                isRecording = true
                val resultData = msg.obj as Intent
                // 开始录制
                if (null == mediaProjection) {
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = manager.getMediaProjection(Activity.RESULT_OK, resultData)
                }

                if (resultData.hasExtra(KEY_RECORD_PARAMS)) {
                    params = resultData.getParcelableExtra(KEY_RECORD_PARAMS) as RecordBean
                }
                startRecord()
            }
            MSG_UNREGISTER_CLIENT -> {
                stopRecord()
            }
        }
        return super.handleMessage(msg)
    }

    /*开启前台*/
    private fun startForeground() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(application.getString(R.string.app_name))
            .setContentText(application.getString(R.string.notification_text_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // 5.0 above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The user-visible name of the channel.
            val channelName = getString(R.string.notification_record_screen_name)
            // The user-visible description of the channel.
            val channelDescription = getString(R.string.notification_record_description)
            val channelImportance = NotificationManager.IMPORTANCE_DEFAULT;

            val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, channelImportance)
            notificationChannel.description = channelDescription
            // 开启指示灯及指示灯颜色
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            // 锁屏显示
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC;

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setClass(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

        val notifyPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(notifyPendingIntent)
        val notification: Notification = builder.build()
        startForeground(hashCode(), notification)
    }

    /*开始录制*/
    private fun startRecord() {
        elapsedStartTimeMs = SystemClock.elapsedRealtime()

        // 发送成功消息到客户端
        val message = Message.obtain(handler, MSG_RECORD_START)
        val data = Bundle()
        data.putLong(KEY_RECORD_START_TIME, elapsedStartTimeMs)
        message.data = data
        message.obj = RES_OK
        sendMessage(message)
    }

    private fun stopRecord() {

    }

}