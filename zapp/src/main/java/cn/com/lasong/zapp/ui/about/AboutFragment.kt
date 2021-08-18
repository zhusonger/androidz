package cn.com.lasong.zapp.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.utils.ZCrypto
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.databinding.FragmentAboutBinding

class AboutFragment : BaseFragment() {

    private lateinit var viewModel: AboutViewModel

    private lateinit var binding: FragmentAboutBinding;
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(AboutViewModel::class.java)
        binding = FragmentAboutBinding.inflate(layoutInflater)
        ViewHelper.setClickAlpha(binding.tvVersion)
        binding.tvVersion.setOnClickListener {
            val key = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlHZk1BMEdDU3FHU0liM0RRRUJBUVVBQTRHTkFEQ0JpUUtCZ1FDcE1KZWRtL21zdklISnVqOVgwVk41aDJRajFZRnRhZWsKdkpXU1lNUlFhdXVzYzRoM0ljS0I1UDBGZUxnSlpnVUtOWUhVczhXVjdya0VPSVNwaTh1cXphcWZ1M1ZZREdJWgowdEd6bzlwMXZiWGU4bDdXbkQrdTdCVnZ3d2xhcmJpcVIyQXVyMUtxUVVxQkgvZkhBT3RsQ0xLeVZsZnkrVSsxCjFMN3F3QU5yeVBZRlZNVE1sd0lEQVFBQgotLS0tLUVORCBQVUJMSUMgS0VZLS0tLS0K"
            ZCrypto.validateClientKey(key)
            ZCrypto.encode("ddd")
        }
        return binding.root
    }
}