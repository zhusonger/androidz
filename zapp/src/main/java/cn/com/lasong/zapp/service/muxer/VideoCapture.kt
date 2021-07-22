package cn.com.lasong.zapp.service.muxer

import cn.com.lasong.media.gles.MEGLHelper
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Author: zhusong
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
    var eglHelper: MEGLHelper? = null


    /**
     * 开始在指定线程捕获视频
     * 创建EGL环境
     */
    suspend fun start() {
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher start : I'm working in thread ${Thread.currentThread().name}")
            if (eglHelper != null) {
                return@withContext
            }
            eglHelper = MEGLHelper.newInstance()
        }
    }

    /**
     * 停止捕获
     * 销毁EGL环境
     */
    suspend fun stop() {
        withContext(videoDispatcher) {
            ILog.d(RecordService.TAG, "VideoDispatcher stop : I'm working in thread ${Thread.currentThread().name}")
            eglHelper?.release()
        }
    }

}