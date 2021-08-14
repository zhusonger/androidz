package cn.com.lasong.zapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.MainViewModel
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.FragmentHomeBinding
import com.bumptech.glide.Glide

class HomeFragment : BaseFragment() {

    // 共享的VM
    private val mainModel : MainViewModel by activityViewModels()
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        ViewHelper.setClickAlpha(binding.cardRecordScreen)
        mainModel.recordingVideo.observe(viewLifecycleOwner, { video->
            val screenshot: ByteArray? = video?.screenshot
            if (null == screenshot) {
                binding.ivShotPlaceholder.visibility = View.VISIBLE
                binding.ivScreenShot.visibility = View.GONE
            } else {
                binding.ivShotPlaceholder.visibility = View.GONE
                binding.ivScreenShot.visibility = View.VISIBLE
                Glide.with(requireContext()).load(screenshot).centerCrop().into(binding.ivScreenShot)
            }
        })

//        ViewHelper.setClickAlpha(binding.cardRecordCamera)
        binding.cardRecordScreen.setOnClickListener {
            findNavController().also {controller ->
                controller.navigate(R.id.action_nav_home_to_nav_screen)
            }
        }

//        binding.cardRecordCamera.setOnClickListener {
//
//        }
        return binding.root
    }
}