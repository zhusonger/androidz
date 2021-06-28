package cn.com.lasong.zapp.ui.home.screen

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.data.RecordBean
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

    lateinit var pickLauncher: ActivityResultLauncher<String>

    // 选择存储路径
    fun selectStoreDir() {
        val saveDir: String = params.value?.saveDir as String
        pickLauncher.launch(saveDir)
    }

    override fun onCleared() {
        super.onCleared()
        pickLauncher.unregister()
    }
}