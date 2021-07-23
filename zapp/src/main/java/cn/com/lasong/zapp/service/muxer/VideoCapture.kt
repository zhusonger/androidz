package cn.com.lasong.zapp.service.muxer

import android.annotation.SuppressLint
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
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * 视频捕获
 */
class VideoCapture : SurfaceTexture.OnFrameAvailableListener {

    companion object {
        const val DISPLAY_NAME = "VideoCapture"
    }
    // 指定线程调度器
    private val videoDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "VideoDispatcher") }
            .asCoroutineDispatcher()

    // EGL环境帮助类
    lateinit var eglHelper: MEGLHelper

    // 视频编码器
    lateinit var videoEncoder: MediaCodec

    // 编码器surface
    lateinit var codecSurface: Surface
    // 画面纹理
    lateinit var surfaceTexture: SurfaceTexture
    // 虚拟屏幕surface
    lateinit var displaySurface: Surface

    // 当前状态
    var state = Mpeg4Muxer.STATE_IDLE

    // 当前编码宽高
    var width: Int = 0
    var height: Int = 0
    var dpi : Int = 1

    // 屏幕数据纹理
    var screenTexture = 0
    // 水印纹理
    var waterTexture = 0

    /**
     * 开始在指定线程捕获视频
     */
    fun start(params: RecordBean) {
        val videoResolution = params.videoResolutionValue
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
        state = Mpeg4Muxer.STATE_START
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
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher start : I'm working in thread ${Thread.currentThread().name}")
            eglHelper = MEGLHelper.newInstance()
            // 把编码器的surface作为gles的输入, 更新gles编码器就会有数据
            eglHelper.setSurface(codecSurface)
            // 创建屏幕录制镜像 surface
            screenTexture = MEGLHelper.glGenOesTexture(1)[0]
            surfaceTexture = SurfaceTexture(screenTexture).also {
                it.setDefaultBufferSize(width, height)
                it.setOnFrameAvailableListener(this@VideoCapture)
            }
            displaySurface = Surface(surfaceTexture)
            // 生成水印纹理
            waterTexture = MEGLHelper.glGen2DTexture(1)[0]
        }
    }

    // 屏幕镜像数据
    var virtualDisplay: VirtualDisplay? = null
    /**
     * 初始化屏幕获取
     */
    suspend fun initMediaProjection(projection: MediaProjection) {
        withContext(Dispatchers.Main) {
            virtualDisplay = projection.createVirtualDisplay(DISPLAY_NAME, width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                displaySurface, null, null)
        }

    }

    /**
     * 销毁EGL环境
     */
    suspend fun unInitEgl() {
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher stop : I'm working in thread ${Thread.currentThread().name}")
            val texture: IntArray = intArrayOf(screenTexture, waterTexture)
            GLES20.glDeleteTextures(texture.size, texture, 0)
            eglHelper.release()
            surfaceTexture.release()
            displaySurface.release()
        }
        withContext(Dispatchers.Main) {
            virtualDisplay?.release()
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        ILog.d(RecordService.TAG, "onFrameAvailable : $surfaceTexture")
    }
}