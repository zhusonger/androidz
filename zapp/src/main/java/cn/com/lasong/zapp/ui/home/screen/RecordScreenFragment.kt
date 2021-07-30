package cn.com.lasong.zapp.ui.home.screen

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.utils.ILog
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.BuildConfig
import cn.com.lasong.zapp.MainViewModel
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.data.RecordKey
import cn.com.lasong.zapp.data.RecordState
import cn.com.lasong.zapp.data.SIZE_MIN_VALUE
import cn.com.lasong.zapp.databinding.FragmentRecordScreenBinding
import cn.com.lasong.zapp.ui.all.ConfirmDialog
import cn.com.lasong.zapp.ui.all.OptionDialog

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/8
 * Description:
 * 屏幕录制页面
 */
class RecordScreenFragment : BaseFragment(), View.OnClickListener {


    private lateinit var binding: FragmentRecordScreenBinding

    // 共享的VM
    private val viewModel : MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordScreenBinding.inflate(layoutInflater, container, false)
        viewModel.params.observe(viewLifecycleOwner, {
            binding.tvFreeSize.text = it.freeSizeDisplay
            when {
                it.appFreeSize >= SIZE_MIN_VALUE -> {
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
            binding.tvVideoClipMode.text = it.clipModeDisplay
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
        binding.llClipMode.setOnClickListener(this)
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

        viewModel.currentState.observe(viewLifecycleOwner, {
            when (it) {
                RecordState.IDLE -> {
                    binding.layoutRecord.isEnabled = true
                    binding.tvRecord.isSelected = false
                }
                RecordState.READY -> {
                    binding.layoutRecord.isEnabled = false
                }
                RecordState.RUNNING -> {
                    binding.layoutRecord.isEnabled = true
                    binding.tvRecord.isSelected = true
                }
                RecordState.STOP -> {
                    binding.layoutRecord.isEnabled = false
                    binding.tvRecord.isSelected = false
                }
                // do nothing
                else -> {
                    ILog.d("currentState : $it")
                }
            }
        })
        return binding.root
    }

    override fun onReStart() {
        super.onReStart()
        viewModel.updateFreeSize()
    }

    override fun onStop() {
        super.onStop()
        viewModel.save()
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
            binding.llClipMode,
            binding.llVideoBitrate,
            binding.llFps,
            binding.llSampleRate,
            binding.llChannel,
            binding.llAudioBitrate,
            binding.llDelay -> {
                val key = when (v) {
                    binding.llVideoDirection -> RecordKey.DIRECTION
                    binding.llResolution -> RecordKey.RESOLUTION
                    binding.llClipMode -> RecordKey.CLIP_MODE
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
            RecordKey.CLIP_MODE -> {
                title = context.getString(R.string.record_video_clip_mode)
                array = resources.getStringArray(R.array.array_clip)
                selectIndex = viewModel.params.value?.clipMode!!
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
        ) { _, position -> viewModel.updateVideo(key, position) }
            .show()
    }

    private fun startRecord() {
        val state = viewModel.currentState.value!!
        if (state == RecordState.RUNNING || state == RecordState.READY) {
            ConfirmDialog.newInstance(context = requireActivity(),
                content = getString(R.string.record_confirm_content_recording),
                listener = { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    viewModel.targetState.value = RecordState.STOP
                }
            }).show()
            return
        }

        val params = viewModel.params.value!!
        if (params.audioEnable) {
            // 先检查是否录音, 录音先请求录音权限
            requestPermissions(
                { isGrant, _ ->
                    if (isGrant) {
                        viewModel.targetState.value = RecordState.READY
                    } else {
                        // 未授权, 弹出弹窗方便可以再次跳转到权限设置
                        val dialog = AlertDialog.Builder(activity).setTitle(R.string.title_default)
                            .setMessage(R.string.record_permission_audio_not_grant)
                            .setPositiveButton(R.string.record_permission_dialog_ok) { _, _ ->
                                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                                startActivity(intent);
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .create()
                        dialog.show()
                    }
                },
                Manifest.permission.RECORD_AUDIO)
            return
        }
        viewModel.targetState.value = RecordState.READY
    }
}