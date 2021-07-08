package cn.com.lasong.zapp.ui.home.screen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.data.RecordKey
import com.tencent.mmkv.MMKV


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/8
 * Description:
 */
class RecordScreenViewModel : ViewModel() {
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

    fun updateFreeSize() {
        val value = params.value
        params.postValue(value)
    }

    fun updateVideo(key : RecordKey, position: Int) {
        val value = params.value
        when(key) {
            RecordKey.DIRECTION -> value?.videoDirection = position
            RecordKey.RESOLUTION -> value?.videoResolution = position
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
}