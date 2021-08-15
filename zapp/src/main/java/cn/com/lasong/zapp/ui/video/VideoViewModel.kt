package cn.com.lasong.zapp.ui.video

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.com.lasong.base.AppManager
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.ZApp.Companion.appInstance
import cn.com.lasong.zapp.database.VideoEntity
import cn.com.lasong.zapp.utils.ZAdapterChanger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoViewModel : ViewModel() {
    companion object {
        const val PAYLOAD_FAVORITE = "Favorite"
        const val PAYLOAD_TITLE = "Title"
    }
    // 分页
    private val pageSize = 10
    // 数据
    val data = mutableListOf<VideoEntity>()
    // 更改监听器
    val changer = MutableLiveData<Pair<ZAdapterChanger, Triple<Int, Int, Any?>>>()

    /**
     * 加载更多
     */
    fun loadMore() {
        viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) {
                val fromIndex = data.lastOrNull()?.id ?: Int.MAX_VALUE
                val dao = appInstance().database.getVideoDao()
                return@withContext dao.queryVideos(fromIndex, pageSize)
            }

            withContext(Dispatchers.Main) {
                val positionStart = data.size
                val itemCount = videos.size
                if (itemCount > 0) {
                    data.addAll(videos)
                }
                changer.value = Pair(ZAdapterChanger.ADD, Triple(positionStart, itemCount, null))
            }
        }
    }

    /**
     * 收藏
     */
    fun clickFavorite(position: Int) {
        val video = data[position]
        video.favorite = !video.favorite
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val dao = appInstance().database.getVideoDao()
                dao.updateVideo(video)
            }

            withContext(Dispatchers.Main) {
                changer.value = Pair(ZAdapterChanger.CHANGE, Triple(position, 1, PAYLOAD_FAVORITE))
            }
        }
    }

    /**
     * 删除视频
     */
    fun deleteVideo(position: Int) {
        val video = data.removeAt(position)
        changer.value = Pair(ZAdapterChanger.REMOVE, Triple(position, 1, null))
        viewModelScope.launch {
            val intentSender = withContext(Dispatchers.IO) {
                runCatching {
                    val dao = appInstance().database.getVideoDao()
                    dao.deleteVideo(video)
                    val file = File(video.path)
                    file.delete()
                    val resolver = ZApp.applicationContext().contentResolver
                    resolver.delete(Uri.parse(video.uri), null, null)
                    return@withContext null
                }.getOrElse {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it is RecoverableSecurityException) {
                        return@withContext it.userAction.actionIntent.intentSender
                    }
                }
            }

            // Android10及以上的处理
            withContext(Dispatchers.Main) {
                intentSender?.let {
                    ActivityCompat.startIntentSenderForResult(
                        AppManager.getInstance().current(), intentSender as IntentSender, 1,
                        null, 0, 0, 0, null
                    )
                }
            }


        }
    }

    /**
     * 更新显示标题
     */
    fun updateTitle(position: Int, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val video = data[position]
            video.title = text
            val dao = appInstance().database.getVideoDao()
            dao.updateVideo(video)
            withContext(Dispatchers.Main) {
                changer.value = Pair(ZAdapterChanger.CHANGE, Triple(position, 1, PAYLOAD_TITLE))
            }
        }
    }
}