package cn.com.lasong.zapp.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.BuildConfig
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.FragmentAboutBinding

class AboutFragment : BaseFragment() {

    private lateinit var viewModel: AboutViewModel

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(AboutViewModel::class.java)
        binding = FragmentAboutBinding.inflate(layoutInflater)
        binding.tvVersion.text = getString(R.string.about_version_name, BuildConfig.VERSION_NAME)
        ViewHelper.setClickAlpha(binding.tvVersion)
        return binding.root
    }
}