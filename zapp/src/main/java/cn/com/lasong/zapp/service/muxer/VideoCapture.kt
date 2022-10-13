package cn.com.lasong.zapp.service.muxer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import cn.com.lasong.media.gles.MEGLHelper
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.data.CLIP_FILL
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.data.RecordVideo
import cn.com.lasong.zapp.service.RecordService
import cn.com.lasong.zapp.service.muxer.render.VideoRender
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * 视频捕获
 */
class VideoCapture(private val scope: CoroutineScope) : SurfaceTexture.OnFrameAvailableListener {

    companion object {
        const val DISPLAY_NAME = "VideoCapture"
        const val CAPTURE_DELAY = 500L
    }

    // 指定线程调度器
    private val videoDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "VideoDispatcher") }
            .asCoroutineDispatcher()

    // EGL环境帮助类
    private lateinit var eglHelper: MEGLHelper

    // 视频编码器
    private lateinit var videoEncoder: MediaCodec

    // 编码器surface
    private lateinit var codecSurface: Surface
    // 画面纹理
    private lateinit var surfaceTexture: SurfaceTexture
    // 虚拟屏幕surface
    private lateinit var displaySurface: Surface

    // 当前状态
    var state = Mpeg4Muxer.STATE_IDLE

    // 当前编码宽高
    lateinit var video: RecordVideo
    private var clipMode: Int = CLIP_FILL

    // 屏幕数据纹理
    private var screenTexture = 0
    // 水印纹理
    private var waterTexture = 0

    // 屏幕镜像数据
    private var virtualDisplay: VirtualDisplay? = null

    // 视频渲染工具
    private lateinit var render: VideoRender

    // 屏幕纹理的矩阵
    private val oesMatrix: FloatArray = FloatArray(16)

    // 截屏保存路径
    private lateinit var screenshotDir: String

    // 单帧时间跨度 ns
    private var minSpanPtsNs: Long = 0
    // 上次渲染的时间戳
    private var lastRenderPtsNs: Long = 0

    // 总帧数
    private var frameCount = 0L
    // 一帧平均处理时长
    private var frameProcessAvgMs = 0L
    // 补帧协程任务
    private var fillFrameJob: Job? = null
    // 截图
    private val captures : Queue<Pair<(String?, ByteArray?)->Unit, Triple<Long, Boolean, Boolean>>> = LinkedBlockingQueue()

    var callback: ICaptureCallback? = null

    /**
     * 开始在指定线程捕获视频
     */
    fun start(params: RecordBean, projection: MediaProjection? = null) {
        if (state != Mpeg4Muxer.STATE_IDLE) {
            ILog.d(RecordService.TAG, "Video Start : $state")
            return
        }
        state = Mpeg4Muxer.STATE_READY
        val videoResolution = params.videoResolutionValue
        screenshotDir = params.screenshotDir!!
        video = videoResolution.copy()
        clipMode = params.clipMode
        minSpanPtsNs = 1000_000_000L / params.videoFpsValue
        val renderWidth = videoResolution.renderWidth
        val renderHeight = videoResolution.renderHeight
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, renderWidth, renderHeight)
        // MediaProjection 使用 surface
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        // 有自己选择的码率就用, 否则使用视频对应的码率
        format.setInteger(
            MediaFormat.KEY_BIT_RATE,
            if (params.videoBitrate > 0) params.videoBitrateValue else videoResolution.bitrate
        )
        // 设置关键帧间隔1s
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.videoFpsValue)
        // 如果是自适应码率, 质量优先
        // 否则尽量接近设置的值
        format.setInteger(
            MediaFormat.KEY_BITRATE_MODE,
            if (params.videoBitrate == 0) MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            else MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
        )
        // 7.0以上设置KEY_PROFILE 才生效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(
                MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
            format.setInteger(
                MediaFormat.KEY_LEVEL,
                MediaCodecInfo.CodecProfileLevel.AVCLevel41)
        }

        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var configure = false
        try {
            videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            configure = true
        } catch (e: Exception) {
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            ILog.d(RecordService.TAG, "Change To BITRATE_MODE_CBR")
        }

        if (!configure) {
            try {
                videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                configure = true
            } catch (e: Exception) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                ILog.d(RecordService.TAG, "Change To BITRATE_MODE_VBR")
            }
        }


        if (!configure) {
            configure = runCatching {
                videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return@runCatching true
            }.getOrDefault(false)
        }

        if (!configure) {
            ILog.e(RecordService.TAG, "Video configure Fail")
            state = Mpeg4Muxer.STATE_IDLE
            return
        }
        codecSurface = videoEncoder.createInputSurface()
        videoEncoder.start()

        scope.launch {
            initEgl()
            if (null != projection) {
                initMediaProjection(projection)
            }
            state = Mpeg4Muxer.STATE_START
        }
    }

    /**
     * 停止捕获
     * 销毁EGL环境
     */
    suspend fun stop() {
        if (state == Mpeg4Muxer.STATE_STOP || state == Mpeg4Muxer.STATE_IDLE) {
            ILog.d(RecordService.TAG, "Video Stop : $state")
            return
        }
        state = Mpeg4Muxer.STATE_STOP
        ILog.d(RecordService.TAG, "VideoCapture stop EGL")
        withContext(videoDispatcher) {
            unInitEgl()
            ILog.d(RecordService.TAG, "VideoCapture stop EGL Done")
        }
        ILog.d(RecordService.TAG, "VideoCapture stop MediaCodec")
        withContext(Dispatchers.IO) {
            try {
                videoEncoder.signalEndOfInputStream()
                // 获取最后的帧数据
                var endOfStream: Boolean
                do {
                    endOfStream = doDrainFrame(true)
                } while (endOfStream)

                videoEncoder.stop()
                videoEncoder.release()
            } catch (e: Exception) {
                ILog.e(RecordService.TAG, e)
            }
        }
        captures.clear()
        ILog.d(RecordService.TAG, "VideoCapture stop MediaCodec Done")
        state = Mpeg4Muxer.STATE_IDLE
    }

    /**
     * 创建EGL环境
     */
    @SuppressLint("Recycle")
    suspend fun initEgl() {
        var watermark: Bitmap? = null
        withContext(Dispatchers.IO) {
            try {
                watermark = BitmapFactory.decodeResource(ZApp.applicationContext().resources, R.drawable.ic_notification_small)
            } catch (t: Throwable) {
                ILog.e(RecordService.TAG, "decodeResource watermark fail", t)
            }
        }
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher start : I'm working in thread ${Thread.currentThread().name}")
            eglHelper = MEGLHelper.newInstance()
            // 把编码器的surface作为gles的输入, 更新gles编码器就会有数据
            eglHelper.setSurface(codecSurface)
            // 创建屏幕录制镜像 surface
            screenTexture = MEGLHelper.glGenOesTexture(1)[0]
            surfaceTexture = SurfaceTexture(screenTexture)
            surfaceTexture.setDefaultBufferSize(video.actualWidth, video.actualHeight)
            surfaceTexture.setOnFrameAvailableListener(this@VideoCapture)
            displaySurface = Surface(surfaceTexture)
            // 生成水印纹理
            waterTexture = MEGLHelper.glGen2DTexture(1)[0]
            // 初始化视频渲染器
            render = VideoRender(eglHelper.glVersion)
            render.init(video.renderWidth, video.renderHeight, video.matrix)
            // 绑定水印纹理
            if (null != watermark) {
                render.updateWaterMark(waterTexture, watermark!!)
            }
        }
    }

    /**
     * 初始化屏幕获取
     */
    private suspend fun initMediaProjection(projection: MediaProjection) {
        // 放在有looper的线程中调用, 否则handler传null会空指针
        withContext(Dispatchers.Main) {
            virtualDisplay = projection.createVirtualDisplay(DISPLAY_NAME, video.actualWidth, video.actualHeight, video.dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                displaySurface, object : VirtualDisplay.Callback() {
                    override fun onPaused() {
                        super.onPaused()
                        ILog.d(RecordService.TAG, "VirtualDisplay onPaused")
                    }

                    override fun onResumed() {
                        super.onResumed()
                        ILog.d(RecordService.TAG, "VirtualDisplay onResumed")
                    }

                    override fun onStopped() {
                        super.onStopped()
                        ILog.d(RecordService.TAG, "VirtualDisplay onStopped")
                    }
                }, null)
        }
    }

    /**
     * 销毁EGL环境
     */
    private suspend fun unInitEgl() {
        fillFrameJob?.cancel("unInitEgl")
        fillFrameJob = null
        withContext(Dispatchers.Main) {
            ILog.d(RecordService.TAG, "release virtualDisplay")
            virtualDisplay?.surface = null
            virtualDisplay?.release()
            virtualDisplay = null
        }
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "release texture")
            val textures: IntArray = intArrayOf(screenTexture, waterTexture)
            MEGLHelper.glDeleteTextures(textures)
            screenTexture = 0
            waterTexture = 0
            render.release()
            surfaceTexture.release()
            displaySurface.release()
            eglHelper.release()
            ILog.d(RecordService.TAG, "release texture done")
        }
    }

    // frame comes one by one, means the callback will not be invoked
    // until SurfaceTexture.updateTexImage() done
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        // 已经停止, 忽略
        if (state == Mpeg4Muxer.STATE_IDLE) {
            return
        }
        // 已经启动了 更新为正在运行
        if (state == Mpeg4Muxer.STATE_START) {
            state = Mpeg4Muxer.STATE_RUNNING
        }
        // 补帧协程
        fillFrameJob?.cancel("NewFrame")
        fillFrameJob = scope.async(start = CoroutineStart.LAZY) {
            try {
                while (isActive) {
                    if (state == Mpeg4Muxer.STATE_IDLE) {
                        break
                    }
                    delay(minSpanPtsNs/1000_000)
                    doFrame(false)
                    ILog.d(RecordService.TAG, "2. Fill Frame Done")
                }
            } catch (e: Exception) {
//                ILog.e(RecordService.TAG, e)
            }
        }

        scope.launch {
            try {
                if (doFrame()) {
                    ILog.d(RecordService.TAG, "1. New Frame Done")
                    fillFrameJob?.start()
                }
            } catch (e: Exception) {
//                ILog.e(RecordService.TAG, e)
            }
        }
    }

    /**
     * 检测纹理是否可用
     */
    private fun isTextureActive(): Boolean {
        if (screenTexture == 0 || surfaceTexture != this@VideoCapture.surfaceTexture) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && surfaceTexture.isReleased) {
            return false
        }
        return true
    }
    /**
     * 执行一帧的渲染与编码
     */
    private suspend fun doFrame(update: Boolean = true, delayFrame:Boolean = true) : Boolean{
        // 间隔时间小于单帧时间间隔, 丢弃这帧
        val ptsNs = Mpeg4Muxer.elapsedPtsNs
        val spanTimeNs = System.nanoTime() - lastRenderPtsNs
        // 丢帧处理
        if (delayFrame && (ptsNs > 0) && (spanTimeNs < minSpanPtsNs)) {
            // 延迟剩余的时间
            val delayMs = (minSpanPtsNs - spanTimeNs) / 1000_000
            delay((delayMs - frameProcessAvgMs).coerceAtLeast(0))
        }
        lastRenderPtsNs = System.nanoTime()
        ILog.d(RecordService.TAG, "doFrame 1: EGL")

        // 1. 渲染屏幕数据
        val ret = withContext(videoDispatcher) {
            if (!isTextureActive()) {
                return@withContext false
            }
            var timestamp = 0L
            // 1. 获取屏幕数据到oes纹理
            if (update) {
                ILog.d(RecordService.TAG, "doFrame 1.1: updateTexImage")
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(oesMatrix) // 纹理矩阵, 一般是校正坐标系统导致的旋转矩阵
                timestamp = surfaceTexture.timestamp
            }
            ILog.d(RecordService.TAG, "doFrame 1.2: doRender")
            // 2. gles绘制->更新时间戳->交换缓冲到编码器surface
            render.doRender(screenTexture, oesMatrix)
            eglHelper.swapBuffer(ptsNs)

            // 2.1 截图
            while (!captures.isEmpty()) {
                val capture: Pair<(String?, ByteArray?)->Unit, Triple<Long, Boolean, Boolean>> = captures.poll()!!
                val block = capture.first
                val delay: Long = capture.second.first
                val cache = capture.second.second
                val bytes = capture.second.third
                withContext(Dispatchers.Main) {
                    delay(delay)
                    val result = doCapture(isCache = cache, isBytes = bytes)
                    // 非缓存且无字节, 更新到媒体库
                    if (!cache && !bytes) {
                        MediaScannerConnection.scanFile(
                            applicationContext(),
                            arrayOf(result as String?),
                            arrayOf("image/png")
                        ) { path, _ ->
                            block(path, null)
                        }
                    }
                    // 字节
                    else if (bytes) {
                        val buffer = result as ByteArray?
                        block(null, buffer)
                    }
                    // 非字节且缓存, 直接返回
                    else {
                        val path = result as String?
                        block(path, null)
                    }
                }
            }
            return@withContext true
        }
        val processEnd = System.nanoTime()
        frameProcessAvgMs = (((frameProcessAvgMs * frameCount) + (processEnd - lastRenderPtsNs) / 1000_000) / ++frameCount)

        ILog.d(RecordService.TAG, "doFrame 2: MediaCodec, render = $ret")
        // 2. 编码器编码并获取数据
        withContext(Dispatchers.IO) {
            while (isActive) {
                if (state == Mpeg4Muxer.STATE_IDLE || !doDrainFrame()) {
                    break
                }
            }
        }
        return ret
    }

    /**
     * 获取编码后的帧数据
     */
    private fun doDrainFrame(endOfStream: Boolean = false): Boolean {
        val info = MediaCodec.BufferInfo()
        val bufferIndex: Int = videoEncoder.dequeueOutputBuffer(info, 0)
        if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            val format: MediaFormat = videoEncoder.outputFormat
            ILog.d(RecordService.TAG, "Video format : $format")
            callback?.onMediaFormat(Mpeg4Muxer.FLAG_VIDEO, format)
        } else if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            ILog.d(RecordService.TAG, "Video INFO_TRY_AGAIN_LATER")
            // 等待结束标志读取结束
            if (endOfStream) {
                return true
            }
            return false
        } else if (bufferIndex < 0) {
            ILog.d(RecordService.TAG, "Video bufferIndex < 0")
        } else if (bufferIndex >= 0) {
            val data: ByteBuffer? = videoEncoder.getOutputBuffer(bufferIndex)
            if (null != data) {
                data.position(info.offset)
                data.limit(info.offset + info.size)
                info.offset = 0
                callback?.onOutputBuffer(Mpeg4Muxer.FLAG_VIDEO, data, info)
            }
            val encoderPtsNs = info.presentationTimeUs * 1000
            ILog.d(RecordService.TAG, "Video getOutputBuffer $bufferIndex : len = ${data?.limit()}," +
                    " encoderPtsNs = $encoderPtsNs")
            videoEncoder.releaseOutputBuffer(bufferIndex, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                ILog.d(RecordService.TAG, "Video BUFFER_FLAG_END_OF_STREAM")
                return false
            }
        }
        return true
    }
    /**
     * 处理旋转矩阵来保证屏幕转向时, 渲染时把画面转回到原来的位置
     * @param target 目标屏幕的方向
     * @param current 当前屏幕的方向
     */
    fun rotate(target: Int, current: Int): Job {
        // 暂停屏幕数据输出
        virtualDisplay?.surface = null
        val width: Int
        val height: Int
        val dpi = video.dpi
        // 横竖屏切换重新设置宽高
        when(current) {
            // 竖屏
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                width = video.actualWidth.coerceAtMost(video.actualHeight)
                height = video.actualWidth.coerceAtLeast(video.actualHeight)
            }
            // 横屏
            else -> {
                width = video.actualWidth.coerceAtLeast(video.actualHeight)
                height = video.actualWidth.coerceAtMost(video.actualHeight)
            }
        }
        // 更新投影矩阵, 横竖评变化需要更换宽高比例
        video.updateMatrix(clipMode, abs(target - current).mod(2) != 0)
        render.updateProjection(video.matrix)

        return scope.launch(videoDispatcher) {
            // !! 这里需要重新创建surface, 原来的surface宽高已经固定
            // 否则virtualDisplay渲染到surface不正确
            surfaceTexture.release()
            displaySurface.release()

            surfaceTexture = SurfaceTexture(screenTexture)
            surfaceTexture.setDefaultBufferSize(width, height)
            surfaceTexture.setOnFrameAvailableListener(this@VideoCapture)
            displaySurface = Surface(surfaceTexture)

            virtualDisplay?.resize(width, height, dpi)
            ILog.d(RecordService.TAG, "rotate $width x $height")
            // 更新旋转矩阵, 把新的屏幕方向转回到开始录制时的方向
            render.rotate(target, current)
            // 恢复屏幕数据输出
            virtualDisplay?.surface = displaySurface
        }
    }

    /**
     * 截图
     */
    fun capture(block: (String?, ByteArray?)->Unit, delay: Long = CAPTURE_DELAY, isCache: Boolean = false, isBytes: Boolean = false) {
        runCatching {
            captures.offer(Pair(block, Triple(delay, isCache, isBytes)))
            ILog.d(RecordService.TAG, "capture : ${System.nanoTime()}, delay = $delay, isCache = $isCache, isBytes = $isBytes")
        }
    }

    /**
     * 截图返回截图路径
     */
    private suspend fun doCapture(isCache: Boolean = false, isBytes: Boolean = false) : Any? {
        if (state != Mpeg4Muxer.STATE_RUNNING) {
            return null
        }
        val renderWidth = video.renderWidth
        val renderHeight = video.renderHeight
        val capacity: Int = renderWidth * renderHeight
        val buffer: IntBuffer = IntBuffer.allocate(capacity)
        withContext(videoDispatcher) {
            GLES20.glReadPixels(
                0, 0, renderWidth, renderHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )
        }
        val pixels = IntArray(capacity)
        val fixPixels: IntArray = pixels.copyOf(capacity)
        buffer.get(pixels)
        withContext(Dispatchers.Default) {
            // glReadPixels output data is reverse
            // simple : RBGA -> ABGR pix[0,0] -> pix[0, h]
            val width: Int = renderWidth
            val height: Int = renderHeight
            for (i in 0 until capacity) {
                val pixel = pixels[i] // pixel dot
                // data is ABGR, we need ARGB
                val pAxGx = pixel and -0xff0100
                val pxxxB = pixel shr 16 and 0xFF
                val pxRxx = pixel and 0xFF shl 16
                val fixPixel = pAxGx or pxRxx or pxxxB
                val row = i / width
                val col = i % width
                val fixIndex = (height - row - 1) * width + col
                fixPixels[fixIndex] = fixPixel
            }
        }
        val result: Any? = withContext(Dispatchers.IO) {
            val bitmap: Bitmap
            // 最大边不超过512
            val maxSize = max(renderWidth, renderHeight);
            val ratio =  if (maxSize > 512) 512.0 / maxSize else 1.0
            try {
                bitmap = Bitmap.createBitmap(
                    fixPixels,
                    (renderWidth * ratio).roundToInt(),
                    (renderHeight * ratio).roundToInt(),
                    Bitmap.Config.ARGB_8888
                )
            } catch (e: Exception) {
                ILog.e(RecordService.TAG, "screenCapture createBitmap", e)
                return@withContext null
            }

            return@withContext runCatching {
                if (isBytes) {
                    val bos = ByteArrayOutputStream()
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    bos.close()
                    return@runCatching bos.toByteArray()
                }
                val simpleDateFormat =
                    SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                val fileName = if (isCache) {
                    "cache_${simpleDateFormat.format(Calendar.getInstance().time)}.png"
                } else {
                    "capture_${simpleDateFormat.format(Calendar.getInstance().time)}.png"
                }
                val shotDir = if (isCache) {
                    File(this@VideoCapture.screenshotDir, ".cache")
                } else {
                    File(this@VideoCapture.screenshotDir)
                }
                if (!shotDir.exists()) {
                    shotDir.mkdirs()
                    if (isCache) {
                        val noMedia = File(shotDir, ".nomedia")
                        noMedia.createNewFile()
                    }
                }
                val file = File(shotDir, fileName)
                if (file.parentFile?.exists() == false) {
                    file.parentFile?.mkdirs()
                }
                val fos = FileOutputStream(file)
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                return@runCatching file.absolutePath
            }.getOrNull()
        }

        ILog.d(RecordService.TAG, "capture $isBytes result: $result")
        return result
    }
}