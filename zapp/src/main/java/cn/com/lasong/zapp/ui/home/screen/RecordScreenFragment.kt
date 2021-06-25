package cn.com.lasong.zapp.ui.home.screen

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.utils.DeviceUtils
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.databinding.FragmentRecordScreenBinding

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/8
 * Description:
 * 屏幕录制页面
 */
class RecordScreenFragment : BaseFragment(), View.OnClickListener {

    private lateinit var viewModel: RecordScreenViewModel

    private lateinit var binding: FragmentRecordScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(RecordScreenViewModel::class.java)
        binding = FragmentRecordScreenBinding.inflate(layoutInflater, container, false)
        viewModel.params.observe(viewLifecycleOwner, {
            binding.tvStoreDir.text = it.saveDirDisplay
            binding.tvVideoDirection.text = it.directionDisplay
            binding.tvVideoResolution.text = it.videoResolutionDisplay
            binding.tvVideoBitrate.text = it.videoBitrateDisplay
            binding.tvVideoFps.text = it.videoFpsDisplay
            binding.stAudio.isChecked = it.audioEnable
            binding.tvSampleRate.text = it.audioSampleRateDisplay
            binding.tvChannel.text = it.audioChannelDisplay
            binding.tvAudioBitrate.text = it.audioBitrateDisplay
        })
        binding.tvStoreDir.setOnClickListener(this)
        binding.llVideoDirection.setOnClickListener(this)
        binding.llResolution.setOnClickListener(this)
        binding.llVideoBitrate.setOnClickListener(this)
        binding.llFps.setOnClickListener(this)
        binding.llAudioParams.setOnClickListener(this)
        binding.llSampleRate.setOnClickListener(this)
        binding.llChannel.setOnClickListener(this)
        binding.llAudioBitrate.setOnClickListener(this)
        binding.layoutRecord.setOnClickListener(this)
        ViewHelper.setClickAlpha(binding.layoutRecord)

        binding.stAudio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llAudioParams.visibility = View.VISIBLE
            } else {
                binding.llAudioParams.visibility = View.GONE
            }
        }
        return binding.root
    }

    override fun onClick(v: View?) {


        when(v) {
            binding.tvStoreDir -> {
                if (DeviceUtils.isN()) {
                    requestPermissions({ isGrant, _ ->
                        if (!isGrant) {
                            return@requestPermissions
                        }
                        viewModel.selectStoreDir()
                    }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    viewModel.selectStoreDir()
                }
            }
        }
    }
}