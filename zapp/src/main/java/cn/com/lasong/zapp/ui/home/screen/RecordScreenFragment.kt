package cn.com.lasong.zapp.ui.home.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.zapp.databinding.FragmentRecordScreenBinding

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/8
 * Description:
 * 屏幕录制页面
 */
class RecordScreenFragment : BaseFragment() {

    private lateinit var viewModel: RecordScreenViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(RecordScreenViewModel::class.java)
        val binding = FragmentRecordScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}