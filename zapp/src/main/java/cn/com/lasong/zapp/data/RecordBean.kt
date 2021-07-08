package cn.com.lasong.zapp.data

import android.media.AudioFormat
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import java.io.File
import java.util.*


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/15
 * Description:
 */
class RecordBean {

    data class Video(val index: Int, var width: Int, var height: Int, var bitrate: Int)
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
        val VIDEO_MOBILE = Video(0, 0, 0, 0)
        val VIDEO_1080P = Video(1, 1920, 1080, 4860_000)
        val VIDEO_720P = Video(2, 1280, 720, 2160_000)
        val VIDEO_480P = Video(3, 854, 480, 960_000)
        val VIDEO_360P = Video(4, 640, 360, 600_000)
        val allResolution = arrayOf(VIDEO_MOBILE, VIDEO_1080P,
            VIDEO_720P, VIDEO_480P, VIDEO_360P)

    }

    // save dir
    var saveDir: String = applicationContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath!!
    var appFreeSize: Long = 0 // free size
        get() {
            field = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = applicationContext().getSystemService<StorageManager>()!!
                val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(File(saveDir))
                storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
            } else {
                val stat = StatFs(saveDir)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                totalBlocks * blockSize
            }
            return field
        }
    var videoDirection = 0 // video direction

    var videoResolution = 0 // resolution index
    var videoBitrate = 0 // bitrate index
    var videoFps = 3 // FPS index

    var audioEnable = true // enable audio
    var audioSampleRate = 1 // sample rate index
    var audioBitrate = 1 // bitrate index
    var audioChannel = 0 // channel index 单双声道

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

//    val videoWidth : Int
//        get() {
//            return when(videoDirection) {
//                DIRECTION_VERTICAL -> videoResolution.second
//                else -> videoResolution.first
//            }
//        }
//
//    val videoHeight : Int
//        get() {
//            return when(videoDirection) {
//                DIRECTION_VERTICAL -> videoResolution.first
//                else -> videoResolution.second
//            }
//        }

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
                val metrics = applicationContext().resources.displayMetrics
                resolution.width = metrics.widthPixels.coerceAtLeast(metrics.heightPixels)
                resolution.height = metrics.heightPixels.coerceAtMost(metrics.widthPixels)
                resolution.bitrate = ((resolution.width * resolution.height * 3.0 / 1000).toInt() * 1000)
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
}