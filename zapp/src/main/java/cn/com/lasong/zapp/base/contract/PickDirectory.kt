package cn.com.lasong.zapp.base.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import java.io.File

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/28
 * Description:
 */
class PickDirectory : ActivityResultContract<String, Uri>() {
    override fun createIntent(context: Context, input: String): Intent {
        val dir = File(input)
        var file = dir
        if (dir.isDirectory) {
            file = File(dir, "file")
        }
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(file));
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data/*FileUtils.getFilePath(applicationContext(), intent?.data)*/
    }
}