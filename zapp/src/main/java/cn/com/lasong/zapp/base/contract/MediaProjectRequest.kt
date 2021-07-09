package cn.com.lasong.zapp.base.contract

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.result.contract.ActivityResultContract

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/9
 * Description:
 * 获取录制权限的请求
 */
class MediaProjectRequest : ActivityResultContract<Void, Intent?>()  {

    override fun createIntent(context: Context, input: Void?): Intent {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return projectionManager.createScreenCaptureIntent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return intent
    }
}