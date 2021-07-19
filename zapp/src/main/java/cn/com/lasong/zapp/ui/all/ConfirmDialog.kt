package cn.com.lasong.zapp.ui.all

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import cn.com.lasong.widget.dialog.ZBottomSheetDialog
import cn.com.lasong.widget.utils.ViewHelper
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.ZApp.Companion.applicationContext
import cn.com.lasong.zapp.databinding.DialogConfirmBinding

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/13
 * Description:
 * 确认弹窗
 */
class ConfirmDialog (context: Context) : ZBottomSheetDialog(context) {
    companion object {
        fun newInstance(context: Context,
                        title: String = applicationContext().getString(R.string.title_default),
                        content: String,
                        listener: DialogInterface.OnClickListener? = null): ConfirmDialog {
            val dialog = ConfirmDialog(context)
            dialog.title = title
            dialog.content = content
            dialog.listener = listener
            return dialog
        }
    }
    private lateinit var binding: DialogConfirmBinding
    lateinit var title: String
    lateinit var content: String
    var listener: DialogInterface.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogConfirmBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.tvTitle.text = title
        binding.tvContent.text = content
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        binding.tvOk.setOnClickListener {
            dismiss()
            listener?.onClick(this, DialogInterface.BUTTON_POSITIVE)
        }
        ViewHelper.setClickAlpha(binding.tvCancel)
        ViewHelper.setClickAlpha(binding.tvOk)
    }
}