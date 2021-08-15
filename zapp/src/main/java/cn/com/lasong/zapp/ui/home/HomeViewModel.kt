package cn.com.lasong.zapp.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.com.lasong.zapp.ZApp.Companion.appInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var lastThumbnail = MutableLiveData<ByteArray?>()

    fun retrieveLastThumbnail() {
        viewModelScope.launch (Dispatchers.IO){
            val dao = appInstance().database.getVideoDao()
            lastThumbnail.postValue(dao.queryLastThumbnail())
        }
    }
}