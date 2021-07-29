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
import android.media.projection.MediaProjection
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import cn.com.lasong.media.gles.MEGLHelper
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
import cn.com.lasong.zapp.service.muxer.render.VideoRender
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.IntBuffer
import java.util.*
import java.util.concurrent.Executors


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * 视频捕获
 */
class VideoCapture(val scope: CoroutineScope) : SurfaceTexture.OnFrameAvailableListener {

    companion object {
        const val DISPLAY_NAME = "VideoCapture"
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
    var width: Int = 0
    var height: Int = 0
    var dpi : Int = 1

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

    lateinit var path: String
    /**
     * 开始在指定线程捕获视频
     */
    fun start(params: RecordBean, projection: MediaProjection? = null) {
        val videoResolution = params.videoResolutionValue
        path = params.saveDir!!
        width = videoResolution.width
        height = videoResolution.height
        dpi = videoResolution.dpi
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
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
        var isConfig = false
        try {
            videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            isConfig = true
        } catch (e: Exception) {
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            ILog.d(RecordService.TAG, "Change To BITRATE_MODE_CBR")
        }

        if (!isConfig) {
            try {
                videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                isConfig = true
            } catch (e: Exception) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                ILog.d(RecordService.TAG, "Change To BITRATE_MODE_VBR")
            }
        }

        if (!isConfig) {
            try {
                videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (e: Exception) {
                ILog.e(e)
                return
            }
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
    fun stop() {
        videoEncoder.stop()
        videoEncoder.release()
        state = Mpeg4Muxer.STATE_STOP
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
            surfaceTexture.setDefaultBufferSize(width, height)
            surfaceTexture.setOnFrameAvailableListener(this@VideoCapture)
            displaySurface = Surface(surfaceTexture)
            // 生成水印纹理
            waterTexture = MEGLHelper.glGen2DTexture(1)[0]
            // 初始化视频渲染器
            render = VideoRender(eglHelper.glVersion)
            render.init(width, height)
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
            virtualDisplay = projection.createVirtualDisplay(DISPLAY_NAME, width, height, dpi,
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
    suspend fun unInitEgl() {
        withContext(Dispatchers.Main) {
            ILog.d(RecordService.TAG, "release virtualDisplay")
            virtualDisplay?.surface = null
            virtualDisplay?.release()
        }
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "release texture")
            val textures: IntArray = intArrayOf(screenTexture, waterTexture)
            MEGLHelper.glDeleteTextures(textures)
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
        ILog.d(RecordService.TAG, "onFrameAvailable")
        scope.launch (videoDispatcher) {
            // 1. 获取屏幕数据到oes纹理
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(oesMatrix) // 纹理矩阵, 一般是校正坐标系统导致的旋转矩阵
            // 2. gles绘制到编码器surface
            render.doFrame(screenTexture, oesMatrix)
            capture()
            val ptsNs = Mpeg4Muxer.elapsedPtsNs
            ILog.d(RecordService.TAG, "elapsedPtsNs : $ptsNs")
            eglHelper.swapBuffer(ptsNs)
            // 3. 编码器获取数据
            withContext(Dispatchers.IO) {

            }
        }
    }

    /**
     * 处理旋转矩阵来保证屏幕转向时, 渲染时把画面转回到原来的位置
     * @param target 目标屏幕的方向
     * @param current 当前屏幕的方向
     */
    fun rotate(target: Int, current: Int) {
        // 暂停屏幕数据输出
        virtualDisplay?.surface = null
        // 横竖屏切换重新设置宽高
        when(current) {
            // 竖屏
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                val newWidth = width.coerceAtMost(height)
                val newHeight = width.coerceAtLeast(height)
                virtualDisplay?.resize(newWidth, newHeight, dpi)
                surfaceTexture.setDefaultBufferSize(newWidth, newHeight)
            }
            // 横屏
            else -> {
                val newWidth = width.coerceAtLeast(height)
                val newHeight = width.coerceAtMost(height)
                virtualDisplay?.resize(newWidth, newHeight, dpi)
                surfaceTexture.setDefaultBufferSize(newWidth, newHeight)
            }
        }
        // 更新旋转矩阵, 把新的屏幕方向转回到开始录制时的方向
        render.rotate(target, current)
        capture = true
        // 恢复屏幕数据输出
        virtualDisplay?.surface = displaySurface
    }

    var capture = true
    var count = 0
    private suspend fun capture() {
        if (capture) {
            capture = false
            val capacity: Int = width * height
            val buffer: IntBuffer = IntBuffer.allocate(capacity)
            // from left bottom, 0, 0
            // from left bottom, 0, 0
            GLES20.glReadPixels(
                0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )

            val pixels = IntArray(capacity)
            val fixPixels: IntArray = pixels.copyOf(capacity)
            buffer.get(pixels)
            withContext(Dispatchers.Default) {
                // glReadPixels output data is reverse
                // simple : RBGA -> ABGR pix[0,0] -> pix[0, h]
                // glReadPixels output data is reverse
                // simple : RBGA -> ABGR pix[0,0] -> pix[0, h]
                val width: Int = width
                val height: Int = height
                for (i in 0 until capacity) {
                    val pixel = pixels[i] // pixel dot
                    // data is ABGR, we need ARGB
                    val pA_G_ = pixel and -0xff0100
                    val p___B = pixel shr 16 and 0xFF
                    val p_R__ = pixel and 0xFF shl 16
                    val fixPixel = pA_G_ or p_R__ or p___B
                    val row = i / width
                    val col = i % width
                    val fixIndex = (height - row - 1) * width + col
                    fixPixels[fixIndex] = fixPixel
                }
            }
            withContext(Dispatchers.IO) {
                try {
                    val bmp = Bitmap.createBitmap(
                        fixPixels,
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    val fos = FileOutputStream(File(path, "capture_$count.png"))
                    bmp?.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()
                } catch (e: Exception) {
                    ILog.e(RecordService.TAG, "screenCapture createBitmap", e)
                }
            }
        }
    }
}