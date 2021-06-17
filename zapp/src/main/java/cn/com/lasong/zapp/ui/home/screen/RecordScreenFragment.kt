package cn.com.lasong.zapp.ui.home.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.ZApp.Companion.JSON
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.databinding.FragmentRecordScreenBinding
import com.tencent.mmkv.MMKV

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/8
 * Description:
 * 屏幕录制页面
 */
class RecordScreenFragment : BaseFragment() {

    private lateinit var viewModel: RecordScreenViewModel

    private lateinit var params: RecordBean

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(RecordScreenViewModel::class.java)
        val binding = FragmentRecordScreenBinding.inflate(layoutInflater, container, false)
        MMKV.defaultMMKV().let {
            it?.getString(ZApp.KEY_RECORD_SAVE, null)
        }.let {
            params = if (it == null) {
                RecordBean()
            } else {
                JSON.fromJson(it, RecordBean::class.java)
            }
        }
        binding.stAudio.setOnCheckedChangeListener { _, isChecked ->
               if (isChecked) {
                   binding.llAudioParams.visibility = View.VISIBLE
               } else {
                   binding.llAudioParams.visibility = View.GONE
               }
        }
        binding.tvStoreDir.text = params.saveDirDisplay
        binding.tvVideoDirection.text = params.directionDisplay
        binding.tvVideoResolution.text = params.videoResolutionDisplay
        binding.tvVideoBitrate.text = params.videoBitrateDisplay
        binding.tvVideoFps.text = params.videoFpsDisplay
        binding.stAudio.isChecked = params.audioEnable
        binding.tvSampleRate.text = params.audioSampleRateDisplay
        binding.tvChannel.text = params.audioChannelDisplay
        binding.tvAudioBitrate.text = params.audioBitrateDisplay
        return binding.root
    }
}