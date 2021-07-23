package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import cn.com.lasong.media.gles.MEGLHelper
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
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
class VideoCapture {

    // 指定线程调度器
    private val videoDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "VideoDispatcher") }
            .asCoroutineDispatcher()

    // EGL环境帮助类
    lateinit var eglHelper: MEGLHelper

    // 视频编码器
    lateinit var videoEncoder: MediaCodec

    lateinit var surface: Surface

    // 当前状态
    var state = Mpeg4Muxer.STATE_IDLE

    /**
     * 开始在指定线程捕获视频
     */
    fun start(params: RecordBean) {
        val videoResolution = params.videoResolutionValue
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            videoResolution.width, videoResolution.height)
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
            ILog.e(e)
        }

        if (!isConfig) {
            try {
                videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                isConfig = true
            } catch (e: Exception) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                ILog.e(e)
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
        surface = videoEncoder.createInputSurface()
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
    suspend fun initEgl() {
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher start : I'm working in thread ${Thread.currentThread().name}")
            eglHelper = MEGLHelper.newInstance()
            eglHelper.setSurface(surface)
        }
    }

    suspend fun unInitEgl() {
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher stop : I'm working in thread ${Thread.currentThread().name}")
            eglHelper.release()
        }
    }
}