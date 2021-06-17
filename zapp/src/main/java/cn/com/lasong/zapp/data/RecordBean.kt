package cn.com.lasong.zapp.data

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Environment
import androidx.annotation.IntDef
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import java.lang.annotation.RetentionPolicy

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/15
 * Description:
 */
class RecordBean {

    companion object {
        const val DIRECTION_VERTICAL = 0
        const val DIRECTION_LANDSCAPE = 1
    }

    var saveDir: String? = null // save dir

    var videoDirection = -1 // video direction

    var videoResolution = Pair(1280, 720)// 720p
    var videoBitrate = 4000_000 // 4Mbps
    var videoFps = 30 // FPS

    var audioEnable = true // enable audio
    var audioSampleRate = 44100 // 44.1KHz
    var audioBitrate = 96_000 // 96kbps
    var audioChannel = AudioFormat.CHANNEL_IN_MONO // 单双声道


    val saveDirDisplay : String
        get() {
            var dir = (saveDir ?: applicationContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath) as String
            dir = dir.replace("/", "/\u2060")
            return dir
        }

    val directionDisplay : String
        get() {
            if (videoDirection < 0) {
                return applicationContext().getString(R.string.record_video_direction_default)
            }
            val array = applicationContext().resources.getStringArray(R.array.array_direction)
            return array[videoDirection]
        }

    val videoWidth : Int
        get() {
            return when(videoDirection) {
                DIRECTION_VERTICAL -> videoResolution.second
                else -> videoResolution.first
            }
        }

    val videoHeight : Int
        get() {
            return when(videoDirection) {
                DIRECTION_VERTICAL -> videoResolution.first
                else -> videoResolution.second
            }
        }

    val videoResolutionDisplay : String
        get() = "%dp".format(videoResolution.second)

    val videoBitrateDisplay : String
        get() = "%dMbps".format(videoBitrate / 1000_000)


    val videoFpsDisplay : String
        get() = "%dFPS".format(videoFps)

    val audioSampleRateDisplay : String
        get() = "%.1fKHz".format(audioSampleRate / 1000.0f)

    val audioChannelDisplay : String
        get() {
            val array = applicationContext().resources.getStringArray(R.array.array_channels)
            return when(audioChannel) {
                AudioFormat.CHANNEL_IN_MONO -> array[0]
                else -> array[1]
            }
        }

    val audioBitrateDisplay : String
        get() = "%dKbps".format(audioBitrate / 1000)
}