package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.view.Surface
import cn.com.lasong.utils.ILog
import cn.com.lasong.utils.TN
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.data.DIRECTION_AUTO
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.database.VideoEntity
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * MP4合成
 */
class Mpeg4Muxer : ICaptureCallback {

    companion object {
        const val FLAG_IDLE = 0
        const val FLAG_AUDIO = 1
        const val FLAG_VIDEO = 2

        const val STATE_IDLE = 0
        const val STATE_READY = 1
        const val STATE_START = 2
        const val STATE_RUNNING = 3
        const val STATE_STOP = 4

        const val SUFFIX_TMP = ".tmp"
        const val SUFFIX_MP4 = ".mp4"
        // 开始&经过的时间
        var startPtsNs = 0L
        val elapsedPtsNs: Long
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
    lateinit var path: String

    // 录制参数
    private lateinit var params: RecordBean

    private var audioCapture: AudioCapture? = null
    private var videoCapture: VideoCapture? = null

    // 合成
    private var muxerFlag = FLAG_IDLE
    private var muxerTarget = FLAG_IDLE
    private var state = STATE_IDLE

    // 当前屏幕的旋转方向
    private var currentRotation = Surface.ROTATION_0

    // 合成器未就绪前丢失的buffer
    private val lostBuffers: Queue<Triple<Int, ByteBuffer, MediaCodec.BufferInfo>> = LinkedBlockingQueue()

    // 视频对象
    var video: VideoEntity? = null

    /*判断音频/视频是否启动*/
    private fun isStart(flag: Int) : Boolean {
        return (muxerFlag and flag) == flag
    }

    /**
     * 开始录制并合成MP4文件
     */
    fun start(params: RecordBean, direction: Int, projection: MediaProjection?) {
        if (state != STATE_IDLE) {
            ILog.d(RecordService.TAG, "State is $state, Video : ${isStart(FLAG_VIDEO)}," +
                    " Audio : ${isStart(FLAG_AUDIO)}")
            return
        }
        state = STATE_READY
        this.params = params
        currentRotation = params.rotation
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val fileName = simpleDateFormat.format(Calendar.getInstance().time)
        params.fileName = "Screen_$fileName"
        val file = File(params.saveDir!!, "${params.fileName!!}$SUFFIX_TMP")
        path = file.absolutePath
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 音频
        if (params.audioEnable) {
            audioCapture = AudioCapture(scope)
            audioCapture?.callback = this
            audioCapture?.start(params)
            muxerTarget = muxerTarget or FLAG_AUDIO
        }

        // 视频
        if (params.videoEnable) {
            videoCapture = VideoCapture(scope)
            videoCapture?.callback = this
            videoCapture?.start(params, projection!!)
            muxerTarget = muxerTarget or FLAG_VIDEO
        }

        video = VideoEntity(path = path, direction = direction, createTime = System.currentTimeMillis())
    }

    /**
     * 停止录制
     */
    fun stop(block: ((String, VideoEntity?)->Unit)? = null) {
        if (state == STATE_STOP || state == STATE_IDLE) {
            return
        }
        state = STATE_STOP
        scope.launch {
            if (isStart(FLAG_AUDIO)) {
                audioCapture?.stop()
                audioCapture = null
                muxerTarget = muxerTarget and FLAG_AUDIO.inv()
                muxerFlag = muxerFlag and FLAG_AUDIO.inv()
            }
            if (isStart(FLAG_VIDEO)) {
                videoCapture?.stop()
                videoCapture = null
                muxerTarget = muxerTarget and FLAG_VIDEO.inv()
                muxerFlag = muxerFlag and FLAG_VIDEO.inv()
            }
            // 等待音视频结束再停止合成
            val result = runCatching {
                muxer.stop()
                muxer.release()
            }
            val file = File(path)
            if (result.isFailure) {
                file.delete()
                withContext(Dispatchers.Main) {
                    TN.show(R.string.muxer_stop_fail)
                }
            } else {
                val dest = File(params.saveDir!!, "${params.fileName!!}$SUFFIX_MP4")
                val ret = file.renameTo(dest)
                if (ret) {
                    path = dest.absolutePath
                }
                video?.duration = elapsedPtsNs / 1000_000_000
                // 扫描视频文件到系统媒体库中
                MediaScannerConnection.scanFile(
                    applicationContext(),
                    arrayOf(path),
                    arrayOf("video/mp4")
                ) { path, uri ->
                    video?.path = path
                    video?.uri = uri.toString()
                    video?.title = file.name.substringBefore(".")
                    if (null != video) {
                        val dao = ZApp.appInstance().database.getVideoDao()
                        dao.insertVideo(video!!)
                    }
                    block?.invoke(path, video)
                }
            }
            startPtsNs = 0
            state = STATE_IDLE
            ILog.d(RecordService.TAG, "Mpeg4Muxer Done, path $path, muxerFlag = $muxerFlag, muxerTarget = $muxerTarget")
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

    /**
     * 截图
     */
    fun capture(block: (String?, ByteArray?)->Unit,
                delay: Long = VideoCapture.CAPTURE_DELAY,
                isCache: Boolean = false,
                isBytes: Boolean = false) {
        videoCapture?.capture(block, delay, isCache, isBytes)
    }

    /**
     * 更新录制中的实时截图
     */
    fun updateRecordingCapture(delay: Long = 1500) {
        capture({ _, bytes ->
            video?.screenshot = bytes
        }, delay = delay, isBytes = true)
    }

    private var audioTrack = 0
    private var videoTrack = 0
    override fun onMediaFormat(flag: Int, format: MediaFormat) {
        when(flag) {
            FLAG_AUDIO -> {
                audioTrack = muxer.addTrack(format)
                muxerFlag = muxerFlag or FLAG_AUDIO
            }
            FLAG_VIDEO -> {
                videoTrack = muxer.addTrack(format)
                muxerFlag = muxerFlag or FLAG_VIDEO
            }
        }
        if (state == STATE_READY && muxerFlag == muxerTarget) {
            muxer.start()
            state = STATE_START
        }
    }

    override fun onOutputBuffer(flag: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
        // Media不要这个配置信息
        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return
        }
        if (state == STATE_START) {
            state = STATE_RUNNING
        }

        // 没有start的合成器, 无法写入数据
        if (muxerFlag != muxerTarget) {
            ILog.d(RecordService.TAG, "Ignore $flag Data, pts : ${info.presentationTimeUs / 1000}ms")
            lostBuffers.offer(Triple(flag, data, info))
            return
        }

        while (lostBuffers.isNotEmpty()) {
            val buffer = lostBuffers.poll()!!
            when(buffer.first) {
                FLAG_AUDIO -> muxer.writeSampleData(buffer.first, buffer.second, buffer.third)
                FLAG_VIDEO -> muxer.writeSampleData(buffer.first, buffer.second, buffer.third)
            }
            ILog.d(RecordService.TAG, "Recover $flag Data, pts : ${buffer.third.presentationTimeUs / 1000}ms")
        }
        when(flag) {
            FLAG_AUDIO -> muxer.writeSampleData(audioTrack, data, info)
            FLAG_VIDEO -> muxer.writeSampleData(videoTrack, data, info)
        }
    }
}