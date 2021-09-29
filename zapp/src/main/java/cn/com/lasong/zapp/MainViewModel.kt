package cn.com.lasong.zapp

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.data.RecordKey
import cn.com.lasong.zapp.data.RecordState
import cn.com.lasong.zapp.data.VersionBean
import cn.com.lasong.zapp.data.remote.NetManager
import cn.com.lasong.zapp.database.VideoEntity
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import java.util.*


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/9
 * Description:
 */
open class MainViewModel : ViewModel() {

    // 当前状态, 主要用于同步UI状态
    val currentState = MutableLiveData(RecordState.IDLE)

    // 目标状态, 走录制的流程
    val targetState = MutableLiveData(RecordState.IDLE)

    // 请求系统录屏返回的Intent
    val captureResult = MutableLiveData<Intent?>()

    // 当前正在录制的视频信息
    val recordingVideo = MutableLiveData<VideoEntity?>()
    val recordingState = MutableLiveData<Boolean>()

    val params = MutableLiveData<RecordBean>().apply {
        MMKV.defaultMMKV().let {
            it?.getString(ZApp.KEY_RECORD_SAVE, null)
        }.let {
            if (it != null) {
                value = ZApp.JSON.fromJson(it, RecordBean::class.java)
            }
            value = value ?: RecordBean()
        }
    }

    // 录制经过的时长 单位s
    val elapsedTimeMs = MutableLiveData(0L)

    // 更新当前状态
    fun updateCurrent(state: RecordState) {
        currentState.value = state
    }

    // 更新目标状态
    fun updateTarget(state: RecordState) {
        targetState.value = state
    }

    fun updateFreeSize() {
        val value = params.value
        params.postValue(value)
    }

    fun updateVideo(key: RecordKey, position: Int) {
        val value = params.value
        when (key) {
            RecordKey.DIRECTION -> value?.videoDirection = position
            RecordKey.RESOLUTION -> value?.videoResolution = position
            RecordKey.CLIP_MODE -> value?.clipMode = position
            RecordKey.VIDEO_BITRATE -> value?.videoBitrate = position
            RecordKey.FPS -> value?.videoFps = position
            RecordKey.SAMPLE_RATE -> value?.audioSampleRate = position
            RecordKey.AUDIO_BITRATE -> value?.audioBitrate = position
            RecordKey.CHANNEL -> value?.audioChannel = position
            RecordKey.DELAY -> value?.delay = position
            else -> Unit
        }
        params.postValue(value)
    }

    fun setAudioEnable(checked: Boolean) {
        val value = params.value
        value?.audioEnable = checked
        params.postValue(value)
    }

    fun save() {
        val json = ZApp.JSON.toJson(params.value)
        MMKV.defaultMMKV().let {
            it?.putString(ZApp.KEY_RECORD_SAVE, json)?.apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
        timer = null
    }

    private var timer: Timer? = null

    fun startTimer(startTime: Long) {
        val current = SystemClock.elapsedRealtime()
        val start = if (startTime > 0) {
            startTime
        } else {
            current
        }
        elapsedTimeMs.value = (current - start) / 1000
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val newValue: Long = (SystemClock.elapsedRealtime() - start) / 1000
                ILog.d("elapsedTime : $newValue")
                elapsedTimeMs.postValue(newValue)
            }
        }, 1000L, 1000L)
    }

    fun stopTimer() {
        timer?.cancel()
    }

    fun updateCapture(result: Intent) {
        captureResult.value = result
    }

    fun updateRecording(video: VideoEntity?, recording: Boolean) {
        recordingVideo.value = video
        recordingState.value = recording
    }

    // 验证客户端和检测更新
    val validateClient = MutableLiveData(0)

    // 更新日志
    val updateLogs = MutableLiveData<VersionBean?>(null)

    // 验证客户端并检测更新
    fun validate() {
        viewModelScope.launch {
            val ret = NetManager.INSTANCE.validateClientKey()
            validateClient.value = ret
            if (ret < 0) {
                return@launch
            }

            val versions = runCatching { NetManager.INSTANCE.checkVersions() }.getOrNull()
            if (versions == null || versions.isEmpty()) {
                return@launch
            }
            // 是否存在强制升级的版本, 如果存在, 就必须升级
            val force = versions.find {
                it.force > 0
            } != null

            var alert  = true
            // 非必须看更新策略是否需要展示
            if (!force) {
                val updateTs = MMKV.defaultMMKV()?.let {
                    return@let it.getLong(ZApp.KEY_UPDATE_TS, 0)
                } ?: 0
                if (updateTs <= 0) {
                    alert = true
                } else {
                    val now = Calendar.getInstance()
                    val year = now.get(Calendar.YEAR)
                    val day = now.get(Calendar.DAY_OF_YEAR)
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = updateTs
                    alert = !(year == calendar.get(Calendar.YEAR)
                            && day == calendar.get(Calendar.DAY_OF_YEAR))
                }
            }

            if (alert) {
                val builder = StringBuilder()
                versions.forEach {
                    builder.append(it.version).appendLine()
                    builder.append(it.logs).appendLine().appendLine()
                }

                updateLogs.value = VersionBean(
                    versions.last().code, versions.last().version,
                    if (force) 1 else 0, builder.toString()
                )
            }

        }
    }
}