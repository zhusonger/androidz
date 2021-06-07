package cn.com.lasong.zapp.ui.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VideoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Video Fragment"
    }
    val text: LiveData<String> = _text
}