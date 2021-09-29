package cn.com.lasong.zapp.ui.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.databinding.FragmentClipVideoBinding

/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/9/29
 * Description: 视频剪辑
 */
class ClipVideoFragment : BaseFragment() {

    private lateinit var binding: FragmentClipVideoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClipVideoBinding.inflate(layoutInflater, container, false)
        ILog.d("arguments : $arguments")
        return binding.root
    }
}