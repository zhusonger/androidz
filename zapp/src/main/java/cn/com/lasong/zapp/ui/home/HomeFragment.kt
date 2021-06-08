package cn.com.lasong.zapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        val binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        ViewHelper.setClickAlpha(binding.cardRecordScreen)
        ViewHelper.setClickAlpha(binding.cardRecordCamera)
        binding.cardRecordScreen.setOnClickListener {
            findNavController().also {controller ->
                controller.createDeepLink()
                controller.navigate(R.id.action_nav_home_to_nav_screen)
            }
        }

        binding.cardRecordCamera.setOnClickListener {

        }
        return binding.root
    }
}