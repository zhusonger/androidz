package cn.com.lasong.zapp.data

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import cn.com.lasong.zapp.ZApp

/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/30
 * Description:
 * 常量定义
 */

val MOBILE_RATIO: Float by lazy {
    val manager = ZApp.applicationContext()
        .getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    manager.defaultDisplay.getRealMetrics(metrics)
    val width = metrics.widthPixels.coerceAtLeast(metrics.heightPixels)
    val height = metrics.heightPixels.coerceAtMost(metrics.widthPixels)
    return@lazy width.toFloat() / height
}

// 空间
const val SIZE_1_KB = 1024
const val SIZE_1_MB = SIZE_1_KB * 1024
const val SIZE_1_GB = SIZE_1_MB * 1024
const val SIZE_MIN_VALUE = 50 * SIZE_1_MB

// 视频方向
const val DIRECTION_AUTO = 0
const val DIRECTION_VERTICAL = 1
const val DIRECTION_LANDSCAPE = 2

// 视频分辨率
val VIDEO_MOBILE = RecordVideo(0)
val VIDEO_1080P = RecordVideo(1, 1920, 1080, 4860_000)
val VIDEO_720P = RecordVideo(2, 1280, 720, 2160_000)
val VIDEO_480P = RecordVideo(3, 854, 480, 960_000)
val VIDEO_360P = RecordVideo(4, 640, 360, 600_000)

// 裁剪模式
const val CLIP_CENTER = 0 // 居中裁剪
const val CLIP_FILL = 1 // 居中填充