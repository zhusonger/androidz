package cn.com.lasong.zapp.ui.home

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.MainViewModel
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.FragmentHomeBinding
import cn.com.lasong.zapp.utils.GlideApp
import cn.com.lasong.zapp.utils.miniThumb
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel


class HomeFragment : BaseFragment() {

    // 共享的VM
    private val mainModel: MainViewModel by activityViewModels()
    private lateinit var homeViewModel: HomeViewModel

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Do something with the video URI
                    mainModel.updateCamera(uri)
                }
            }
        };

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.retrieveLastThumbnail()
        val binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        ViewHelper.setClickAlpha(binding.cardRecordScreen)
        mainModel.recordingVideo.observe(viewLifecycleOwner) { video ->
            val screenshot: ByteArray? = video?.screenshot
            if (null != screenshot) {
                binding.ivShotPlaceholder.visibility = View.GONE
                binding.ivScreenShot.visibility = View.VISIBLE
                GlideApp.with(requireContext()).load(screenshot).miniThumb()
                    .into(binding.ivScreenShot)
            } else {
                binding.ivShotPlaceholder.visibility = View.VISIBLE
                binding.ivScreenShot.visibility = View.GONE
            }
        }
        mainModel.recordingState.observe(viewLifecycleOwner) { recording ->
            binding.ivRecording.visibility = if (recording) View.VISIBLE else View.GONE
            if (recording) {
                val model =
                    ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL)
                        .build()
                val background = MaterialShapeDrawable(model).apply {
                    setTint(Color.RED)
                }
                val animation = AlphaAnimation(1.0f, 0.2f)
                animation.duration = 700
                animation.repeatCount = -1
                animation.interpolator = LinearInterpolator()
                binding.ivRecording.background = background
                binding.ivRecording.startAnimation(animation)
            } else {
                binding.ivRecording.clearAnimation()
                binding.ivRecording.background = null
            }
        }

        homeViewModel.lastThumbnail.observe(viewLifecycleOwner) { screenshot ->
            if (null != screenshot && binding.ivShotPlaceholder.visibility == View.VISIBLE) {
                binding.ivShotPlaceholder.visibility = View.GONE
                binding.ivScreenShot.visibility = View.VISIBLE
                GlideApp.with(requireContext()).load(screenshot).miniThumb()
                    .into(binding.ivScreenShot)
            }
        }


        binding.cardRecordScreen.setOnClickListener {
            findNavController().also { controller ->
                controller.navigate(R.id.action_nav_home_to_nav_screen)
            }
        }
        binding.cardRecordCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            }
            cameraLauncher.launch(intent)
        }
        return binding.root
    }
}