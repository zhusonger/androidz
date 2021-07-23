package cn.com.lasong.zapp.service.muxer

import android.media.MediaMuxer
import cn.com.lasong.utils.ILog
import cn.com.lasong.utils.TN
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.data.RecordBean
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
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

    /*判断音频/视频是否启动*/
    fun isStart(flag: Int) : Boolean {
        return (muxerFlag and flag) == flag
    }

    /**
     * 开始录制并合成MP4文件
     */
    fun start(params: RecordBean) {
        this.params = params
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val fileName = simpleDateFormat.format(Calendar.getInstance().time)
        params.fileName = "Screen_$fileName.mp4"
        val file = File(params.saveDir!!, params.fileName!!)
        path = file.absolutePath
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 音频
        if (params.audioEnable) {
            audioCapture = AudioCapture()
            audioCapture?.start(params)
        }

        // 视频
        if (params.videoEnable) {
            videoCapture = VideoCapture()
            videoCapture?.start(params)
            scope.launch {
                videoCapture?.initEgl()
            }
        }
    }

    /**
     * 停止录制
     */
    fun stop() {
        audioCapture?.stop()
        videoCapture?.stop()
        scope.launch {
            videoCapture?.unInitEgl()
        }
        try {
            muxer.stop()
            muxer.release()
        } catch (e: Exception) {
            ILog.e(e)
            val file = File(path)
            file.delete()
            TN.show(R.string.muxer_stop_fail)
        }
        audioCapture?.state = STATE_IDLE
        videoCapture?.state = STATE_IDLE
    }

    fun cancel() {
        scope.cancel()
    }
}