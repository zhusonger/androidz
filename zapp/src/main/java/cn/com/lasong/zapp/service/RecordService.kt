package cn.com.lasong.zapp.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.os.PowerManager.WakeLock
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.MainActivity
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.data.DIRECTION_AUTO
import cn.com.lasong.zapp.data.DIRECTION_LANDSCAPE
import cn.com.lasong.zapp.data.DIRECTION_VERTICAL
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.muxer.Mpeg4Muxer
import kotlinx.coroutines.*
import java.io.File


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
        const val MSG_QUERY_LAST_RECORD = 2
        const val MSG_RECORD_STOP = 3

        const val KEY_RECORDING = "recording"
        const val KEY_MEDIA_DATA_EXIST = "media_data_exist"
        const val KEY_RECORD_PARAMS = "record_params"
        const val KEY_RECORD_START_TIME = "record_start_time"

        const val CHANNEL_ID = "RECORD_VIDEO_CHANNEL_ID"

        const val TAG = "RecordService"
        const val LOCK_NAME = "$TAG:wakeLock"
    }

    // 是否正在录制
    private var isRecording = false
    // 录制参数
    private var params: RecordBean? = null

    // 录制屏幕对象
    private var mediaProjection: MediaProjection? = null
    private var mediaData: Intent? = null

    // 录制的启动时间戳
    private var elapsedStartTimeMs: Long = 0

    // MP4合成器
    private val muxer: Mpeg4Muxer = Mpeg4Muxer()
    // 协程域, SupervisorJob 一个子协程出错, 不会影响其他的子协程, Job会传递错误
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 监听MP停止时自动停止录制
    private val callback : MediaProjection.Callback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            stopRecord()
            mediaProjection = null
            mediaData = null
        }
    }
    // onConfigurationChanged 2个横屏状态互相转换的时候不会回调, 监听系统广播
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            ILog.d(TAG,"onConfigurationChanged BroadcastReceiver")
            val rotation = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            muxer.updateOrientation(rotation)
        }
    }

    // 常亮控制
    private var wakeLock: WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        // 启动时删除临时文件
        scope.launch(Dispatchers.IO) {
            val bean = RecordBean()
            val saveDir = File(bean.saveDir!!)
            val tmpFiles = saveDir.list { _, name ->
                null != name && name.endsWith(Mpeg4Muxer.SUFFIX_TMP)
            }
            tmpFiles?.forEach {
                val file = File(saveDir, it)
                file.delete()
                ILog.d(TAG, "delete tmp file : ${file.absolutePath}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ILog.d(TAG,"onDestroy")
        unregisterReceiver(receiver)
        /*销毁时取消协程域*/
        muxer.cancel()
        scope.cancel()
        mediaProjection?.stop()
        mediaProjection = null
        mediaData = null
    }

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            // 1. 返回正在录制的查询结果
            MSG_QUERY_RECORD, MSG_QUERY_LAST_RECORD -> {
                val message = Message.obtain(handler, msg.what)
                message.obj = mapOf(KEY_RECORDING to isRecording,
                    KEY_MEDIA_DATA_EXIST to (mediaData != null),
                    KEY_RECORD_PARAMS to params,
                    KEY_RECORD_START_TIME to elapsedStartTimeMs)
                sendMessage(message)
            }
            // 2. 开始录制
            MSG_RECORD_START -> {
                startForeground()
                isRecording = true
                val data = msg.obj as Intent
                if (data.hasExtra(KEY_RECORD_PARAMS)) {
                    val params = data.getParcelableExtra(KEY_RECORD_PARAMS) as RecordBean?
                    this.params = params?.copy()
                    data.removeExtra(KEY_RECORD_PARAMS)
                }
                // 开始录制
                if (null == mediaData) {
                    mediaData = data
                }
                val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                // TODO: 2021/8/1 还没仔细研究, 有些设备需要这样重新获取, 有些需要复用之前的
                val projection = runCatching {
                    return@runCatching manager.getMediaProjection(Activity.RESULT_OK, mediaData!!)
                }.getOrNull()
                // 能正确获取到就更新, 否则报错了就用之前那个
                if (null != projection) {
                    mediaProjection = projection
                }
                mediaProjection?.registerCallback(callback, null)
                if (null != mediaProjection) {
                    startRecord()
                } else {
                    stopRecord()
                }
            }
            // 3. 停止录制
            MSG_RECORD_STOP -> {
                stopRecord()
                isRecording = false
                elapsedStartTimeMs = 0
                params = null
                stopForeground(true)
                stopSelf()
                // 发送消息到客户端
                val message = Message.obtain(handler, MSG_RECORD_STOP)
                message.obj = RES_OK
                sendMessage(message)
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

        // 获取常亮锁
        if (null == wakeLock) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                LOCK_NAME
            )
            wakeLock?.acquire(300)
        }

        // 发送成功消息到客户端
        val message = Message.obtain(handler, MSG_RECORD_START)
        val data = Bundle()
        data.putLong(KEY_RECORD_START_TIME, elapsedStartTimeMs)
        message.data = data
        message.obj = RES_OK
        sendMessage(message)

        val params: RecordBean = params?.copy()!!
        var direction = params.videoDirection
        val resolution = params.videoResolutionValue
        // 启动时使用屏幕的方向来设置宽高
        if (direction == DIRECTION_AUTO) {
            val rotation = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            params.rotation = rotation
            ILog.d(TAG, "default rotation : $rotation")
            direction = when(rotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    DIRECTION_VERTICAL
                }
                else -> {
                    DIRECTION_LANDSCAPE
                }
            }
        }
        ILog.d(TAG, "direction: $direction, origin: ${params.videoDirection}, resolution : $resolution")
        // 校正方向
        resolution.coerceDirection(direction)
        // 调整宽高靠近手机分辨率比例
        resolution.alignToMobileRatio()
        // 更新投影矩阵
        resolution.updateMatrix(params.clipMode)
        muxer.start(params, mediaProjection)
    }

    /*停止录制*/
    private fun stopRecord() {
        ILog.d(TAG, "stopRecord")
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
        }
        muxer.stop()
        mediaProjection?.unregisterCallback(callback)
    }
}