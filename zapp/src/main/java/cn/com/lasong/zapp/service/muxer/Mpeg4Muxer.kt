package cn.com.lasong.zapp.service.muxer

import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.view.Surface
import cn.com.lasong.utils.ILog
import cn.com.lasong.utils.TN
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.data.DIRECTION_AUTO
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * MP4合成
 */
class Mpeg4Muxer {

    companion object {
        const val FLAG_IDLE = 0
        const val FLAG_AUDIO = 1
        const val FLAG_VIDEO = 2

        const val STATE_IDLE = 0
        const val STATE_START = 1
        const val STATE_RUNNING = 2
        const val STATE_STOP = 3

        // 开始&经过的时间
        var startPtsNs = 0L
        var elapsedPtsNs = 0L
            get() {
                val now = System.nanoTime()
                if (startPtsNs <= 0) {
                    startPtsNs = now
                }
                return now - startPtsNs
            }
    }

    // 协程域, SupervisorJob 一个子协程出错, 不会影响其他的子协程, Job会传递错误
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 视频合成器
    private lateinit var muxer: MediaMuxer

    // 录制文件路径
    private lateinit var path: String

    // 录制参数
    private lateinit var params: RecordBean

    private var audioCapture: AudioCapture? = null
    private var videoCapture: VideoCapture? = null

    // 合成
    private var muxerFlag = FLAG_IDLE

    // 当前屏幕的旋转方向
    private var currentRotation = Surface.ROTATION_0

    /*判断音频/视频是否启动*/
    fun isStart(flag: Int) : Boolean {
        return (muxerFlag and flag) == flag
    }

    /**
     * 开始录制并合成MP4文件
     */
    fun start(params: RecordBean, projection: MediaProjection?) {
        if (isStart(FLAG_VIDEO) || isStart(FLAG_AUDIO)) {
            ILog.d(RecordService.TAG, "is Start video : ${isStart(FLAG_VIDEO)}," +
                    " audio : ${isStart(FLAG_AUDIO)}")
            return
        }
        this.params = params
        currentRotation = params.rotation
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val fileName = simpleDateFormat.format(Calendar.getInstance().time)
        params.fileName = "Screen_$fileName.mp4"
        val file = File(params.saveDir!!, params.fileName!!)
        path = file.absolutePath
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 音频
        if (params.audioEnable) {
            audioCapture = AudioCapture(scope)
            audioCapture?.start(params)
            muxerFlag = muxerFlag or FLAG_AUDIO
        }

        // 视频
        if (params.videoEnable) {
            videoCapture = VideoCapture(scope)
            videoCapture?.start(params, projection!!)
            muxerFlag = muxerFlag or FLAG_VIDEO
        }
    }

    /**
     * 停止录制
     */
    fun stop() {
        scope.launch {
            if (isStart(FLAG_AUDIO)) {
                audioCapture?.stop()
                muxerFlag = muxerFlag and FLAG_AUDIO.inv()
            }
            if (isStart(FLAG_VIDEO)) {
                videoCapture?.stop()
                muxerFlag = muxerFlag and FLAG_VIDEO.inv()
            }
            // 等待音视频结束再停止合成
            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                val file = File(path)
                file.delete()
                withContext(Dispatchers.Main) {
                    TN.show(R.string.muxer_stop_fail)
                }
            }
            audioCapture?.state = STATE_IDLE
            videoCapture?.state = STATE_IDLE
            startPtsNs = 0
        }
    }

    /*取消协程域*/
    fun cancel() {
        scope.cancel()
    }

    /*屏幕方向更改*/
    fun updateOrientation(rotation: Int) {
        if (!isStart(FLAG_VIDEO)) {
            return
        }
        val target = params.rotation
        if (params.videoDirection == DIRECTION_AUTO && rotation != currentRotation) {
            currentRotation = rotation
            ILog.d(RecordService.TAG, "change Orientation target : $target, current : $rotation")
            videoCapture?.rotate(target, rotation)
        }
    }
}