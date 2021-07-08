package cn.com.lasong.zapp.ui.home.screen

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.data.RecordKey
import cn.com.lasong.zapp.databinding.FragmentRecordScreenBinding
import cn.com.lasong.zapp.ui.home.OptionDialog

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(RecordScreenViewModel::class.java)
        binding = FragmentRecordScreenBinding.inflate(layoutInflater, container, false)
        viewModel.params.observe(viewLifecycleOwner, {
            binding.tvFreeSize.text = it.freeSizeDisplay
            when {
                it.appFreeSize >= RecordBean.SIZE_MIN_VALUE -> {
                    binding.tvFreeSize.setTextColor(
                        ContextCompat.getColor(
                            applicationContext(),
                            R.color.colorPrimaryDark
                        )
                    )
                }
                else -> {
                    binding.tvFreeSize.setTextColor(
                        ContextCompat.getColor(
                            applicationContext(),
                            R.color.colorError
                        )
                    )
                }
            }
            binding.tvVideoDirection.text = it.directionDisplay
            binding.tvVideoResolution.text = it.videoResolutionDisplay
            binding.tvVideoBitrate.text = it.videoBitrateDisplay
            binding.tvVideoFps.text = it.videoFpsDisplay
            binding.stAudio.isChecked = it.audioEnable
            binding.tvSampleRate.text = it.audioSampleRateDisplay
            binding.tvChannel.text = it.audioChannelDisplay
            binding.tvAudioBitrate.text = it.audioBitrateDisplay
            binding.tvDelay.text = it.delayDisplay
            if (it.audioEnable) {
                binding.llAudioParams.visibility = View.VISIBLE
            } else {
                binding.llAudioParams.visibility = View.GONE
            }
        })
        binding.llFreeSize.setOnClickListener(this)
        binding.llVideoDirection.setOnClickListener(this)
        binding.llResolution.setOnClickListener(this)
        binding.llVideoBitrate.setOnClickListener(this)
        binding.llFps.setOnClickListener(this)

        binding.llSampleRate.setOnClickListener(this)
        binding.llChannel.setOnClickListener(this)
        binding.llAudioBitrate.setOnClickListener(this)
        binding.llDelay.setOnClickListener(this)
        binding.layoutRecord.setOnClickListener(this)
        ViewHelper.setClickAlpha(binding.layoutRecord)

        binding.stAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAudioEnable(isChecked)
        }

        return binding.root
    }

    override fun onReStart() {
        super.onReStart()
        viewModel.updateFreeSize()
    }

    override fun onClick(v: View) {
        val context = v.context
        when (v) {
            binding.llFreeSize -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    val storageIntent = Intent().apply {
                        action = StorageManager.ACTION_MANAGE_STORAGE
                    }
                    startActivity(storageIntent)
                }
            }
            binding.llVideoDirection,
            binding.llResolution,
            binding.llVideoBitrate,
            binding.llFps,
            binding.llSampleRate,
            binding.llChannel,
            binding.llAudioBitrate,
            binding.llDelay -> {
                val key = when (v) {
                    binding.llVideoDirection -> RecordKey.DIRECTION
                    binding.llResolution -> RecordKey.RESOLUTION
                    binding.llVideoBitrate -> RecordKey.VIDEO_BITRATE
                    binding.llFps -> RecordKey.FPS
                    binding.llSampleRate -> RecordKey.SAMPLE_RATE
                    binding.llChannel -> RecordKey.CHANNEL
                    binding.llAudioBitrate -> RecordKey.AUDIO_BITRATE
                    binding.llDelay -> RecordKey.DELAY
                    else -> RecordKey.NONE
                }
                showRecordOptions(context, key)
            }

            binding.layoutRecord -> {
                startRecord()
            }
        }
    }

    /*显示视频选项*/
    private fun showRecordOptions(context: Context, key: RecordKey) {
        val title: String
        val array: Array<String>
        val selectIndex: Int

        when (key) {
            RecordKey.DIRECTION -> {
                title = context.getString(R.string.record_video_direction)
                array = resources.getStringArray(R.array.array_direction)
                selectIndex = viewModel.params.value?.videoDirection!!
            }
            RecordKey.RESOLUTION -> {
                title = context.getString(R.string.record_video_resolution)
                array = resources.getStringArray(R.array.array_resolution)
                selectIndex = viewModel.params.value?.videoResolution!!
            }
            RecordKey.VIDEO_BITRATE -> {
                title = context.getString(R.string.record_bitrate)
                array = resources.getStringArray(R.array.array_video_bitrate)
                selectIndex = viewModel.params.value?.videoBitrate!!
            }
            RecordKey.FPS -> {
                title = context.getString(R.string.record_video_fps)
                array = resources.getStringArray(R.array.array_fps)
                selectIndex = viewModel.params.value?.videoFps!!
            }
            RecordKey.SAMPLE_RATE -> {
                title = context.getString(R.string.record_sample_rate)
                array = resources.getStringArray(R.array.array_sample_rate)
                selectIndex = viewModel.params.value?.audioSampleRate!!
            }
            RecordKey.AUDIO_BITRATE -> {
                title = context.getString(R.string.record_bitrate)
                array = resources.getStringArray(R.array.array_audio_bitrate)
                selectIndex = viewModel.params.value?.audioBitrate!!
            }
            RecordKey.CHANNEL -> {
                title = context.getString(R.string.record_channel)
                array = resources.getStringArray(R.array.array_channels)
                selectIndex = viewModel.params.value?.audioChannel!!
            }
            RecordKey.DELAY -> {
                title = context.getString(R.string.record_delay)
                array = resources.getStringArray(R.array.array_delay)
                selectIndex = viewModel.params.value?.delay!!
            }
            else -> {
                title = ""
                array = emptyArray()
                selectIndex = 0
            }
        }
        OptionDialog.newInstance(
            context,
            title,
            array,
            selectIndex
        )
        { _, position -> viewModel.updateVideo(key, position) }
            .show()
    }

    private fun startRecord() {

    }

    override fun onStop() {
        super.onStop()
        viewModel.save()
    }
}