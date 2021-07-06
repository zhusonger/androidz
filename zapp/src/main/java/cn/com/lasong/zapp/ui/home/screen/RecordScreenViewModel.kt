package cn.com.lasong.zapp.ui.home.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    lateinit var pickLauncher: ActivityResultLauncher<Uri?>

    // 选择存储路径
    fun selectStoreDir(context: Context = applicationContext()) {
        val saveDir: String = params.value?.saveDir as String
        val file = File(saveDir/*, "video.mp4"*/)

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(FileProvider.getUriForFile(applicationContext(), BuildConfig.FILE_PROVIDER_AUTHOR, file), "*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (context == applicationContext()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent);

//        val input = DocumentsContract.buildDocumentUri(BuildConfig.FILE_PROVIDER_AUTHOR,
//            DocumentsContract.getDocumentId(Uri.parse(file.absolutePath))); //folder Uri
//        val uri = FileProvider.getUriForFile(applicationContext(), BuildConfig.FILE_PROVIDER_AUTHOR, file)
//        val input = DocumentFile.fromTreeUri(applicationContext(), Uri.parse(file.absolutePath))
//        pickLauncher.launch(DocumentFile.fromFile(file).uri)
    }

    override fun onCleared() {
        super.onCleared()
        pickLauncher.unregister()
    }
}