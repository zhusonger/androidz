package cn.com.lasong.zapp.ui.home.screen

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.com.lasong.utils.DeviceUtils
import cn.com.lasong.zapp.BuildConfig
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.data.RecordBean
import com.tencent.mmkv.MMKV
import java.io.File


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

    // 选择存储路径
    fun selectStoreDir() {
        val saveDir: String = params.value?.saveDir as String

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri : Uri
        if (DeviceUtils.isN()) {
            uri = Uri.fromFile(File(saveDir))
        } else {
            uri  = FileProvider.getUriForFile(applicationContext(), BuildConfig.FILE_PROVIDER_AUTHOR, File(saveDir))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.setDataAndType(uri, "*/*")
        Intent.createChooser(intent, "视频存储")
        applicationContext().startActivity(intent)
    }
}