package cn.com.lasong.zapp.data

import android.content.Context
import android.media.AudioFormat
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.getSystemService
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.*


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/15
 * Description:
 */
@Parcelize
class RecordBean(var saveDir: String?,
                 var videoEnable: Boolean = true, // 视频是否可用
                 private var _appFreeSize: Long = 0,// video direction
                 var videoDirection: Int = 0,// resolution index
                 var videoResolution: Int = 0,// bitrate index
                 var videoBitrate: Int = 0,// FPS index
                 var videoFps: Int = 3,// enable audio
                 var audioEnable: Boolean = true,// sample rate index
                 var audioSampleRate: Int = 1,// bitrate index
                 var audioBitrate: Int = 1,// channel index 单双声道
                 var audioChannel: Int = 0,// 录制开始倒计时
                 var fileName: String? = null, //  文件名
                 var delay: Int = 0, // 延时开始录制时间, 单位s
                 var rotation: Int = Surface.ROTATION_0 // 视频旋转角度, 默认未旋转, 只有在videoDirection为DIRECTION_AUTO时生效
) : Parcelable {

    constructor() : this(null)

    @Parcelize
    data class Video(val index: Int, var _width: Int, var _height: Int, var bitrate: Int, var dpi: Int = 1) : Parcelable {
        // 做16位对齐
        @IgnoredOnParcel
        val width: Int
            get(){
                val offset = _width % 16
                if (offset != 0) {
                    return _width + (16 - offset)
                }
                return _width
            }

        @IgnoredOnParcel
        val height: Int
            get(){
                val offset = _height % 16
                if (offset != 0) {
                    return _height + (16 - offset)
                }
                return _height
            }
    }

    companion object {
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
        private val VIDEO_MOBILE = Video(0, 0, 0, 0)
        private val VIDEO_1080P = Video(1, 1920, 1080, 4860_000)
        private val VIDEO_720P = Video(2, 1280, 720, 2160_000)
        private val VIDEO_480P = Video(3, 854, 480, 960_000)
        private val VIDEO_360P = Video(4, 640, 360, 600_000)
        val allResolution = arrayOf(VIDEO_MOBILE, VIDEO_1080P,
            VIDEO_720P, VIDEO_480P, VIDEO_360P)

    }

    init {
        // save dir
        saveDir = applicationContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath
    }


    var appFreeSize: Long  // free size
        get() {
            _appFreeSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = applicationContext().getSystemService<StorageManager>()!!
                val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(File(saveDir!!))
                storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
            } else {
                val stat = StatFs(saveDir)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                totalBlocks * blockSize
            }
            return _appFreeSize
        }
        set(value) {
            _appFreeSize = value
        }

    val freeSizeDisplay : String
        get() {
            return when {
                appFreeSize > SIZE_1_GB -> {
                    "%.2fGB".format(appFreeSize * 1.0 / SIZE_1_GB)
                }
                appFreeSize > SIZE_1_MB -> {
                    "%.2fMB".format(appFreeSize * 1.0 / SIZE_1_MB)
                }
                appFreeSize > SIZE_1_KB -> {
                    "%.2fKB".format(appFreeSize * 1.0 / SIZE_1_KB)
                }
                else -> {
                    "%.2fBytes".format(appFreeSize * 1.0)
                }
            }

        }

    val directionDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_direction)
            return array[videoDirection]
        }

    val videoResolutionDisplay : String
        get() {
            val resolution = allResolution[videoResolution]
            return when(resolution.height) {
                0 -> {
                    applicationContext().getString(R.string.record_video_resolution_default)
                } else -> {
                    "%dp".format(resolution.height)
                }
            }
        }
    val videoResolutionValue : Video
        get() {
            val resolution = allResolution[videoResolution]
            if (resolution.index == 0 && resolution.width == 0) {
                val manager = applicationContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                manager.defaultDisplay.getRealMetrics(metrics)
                resolution._width = metrics.widthPixels.coerceAtLeast(metrics.heightPixels)
                resolution._height = metrics.heightPixels.coerceAtMost(metrics.widthPixels)
                resolution.bitrate = ((resolution.width * resolution.height * 3.0 / 1000).toInt() * 1000)
                resolution.dpi = metrics.density.toInt()
            }
            return resolution
        }

    val videoBitrateDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_video_bitrate)
            return array[videoBitrate]
        }
    val videoBitrateValue : Int
        get() {
            val array = applicationContext().resources.getIntArray(R.array.array_video_bitrate_value)
            return array[videoBitrate]
        }

    val videoFpsDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_fps)
            return array[videoFps]
        }
    val videoFpsValue : Int
        get() {
            val array = applicationContext().resources.getIntArray(R.array.array_fps_value)
            return array[videoFps]
        }

    val audioSampleRateDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_sample_rate)
            return array[audioSampleRate]
        }
    val audioSampleRateValue : Int
        get() {
            val array = applicationContext().resources.getIntArray(R.array.array_sample_rate_value)
            return array[audioSampleRate]
        }

    val audioBitrateDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_audio_bitrate)
            return array[audioBitrate]
        }
    val audioBitrateValue : Int
        get() {
            val array = applicationContext().resources.getIntArray(R.array.array_audio_bitrate_value)
            return array[audioBitrate]
        }

    val audioChannelDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_channels)
            return array[audioChannel]
        }
    val audioChannelValue : Int
        get() {
            return when(audioChannel) {
                1 -> AudioFormat.CHANNEL_IN_STEREO
                else -> AudioFormat.CHANNEL_IN_MONO
            }
        }

    val audioChannelCountValue : Int
        get() {
            return when(audioChannel) {
                1 -> 2
                else -> 1
            }
        }

    val delayDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_delay)
            return array[delay]
        }
    val delayValue : Int
        get() {
            val array = applicationContext().resources.getIntArray(R.array.array_delay_value)
            return array[delay]
        }
}