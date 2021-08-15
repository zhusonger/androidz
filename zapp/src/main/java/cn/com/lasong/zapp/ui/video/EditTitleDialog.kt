package cn.com.lasong.zapp.ui.video

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import cn.com.lasong.utils.KeyboardUtil
import cn.com.lasong.utils.TN
import cn.com.lasong.widget.dialog.ZBottomSheetDialog
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.ViewEditContentBinding

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/8/15
 * Description:
 * 编辑标题弹窗
 */
class EditTitleDialog(
    context: Context,
    private val title: String? = null,
    private val block: ((String?) -> Unit)? = null
) : ZBottomSheetDialog(context) {

    private lateinit var binding: ViewEditContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewEditContentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        binding.edtContent.setHint(R.string.video_edit_title_hint)
        if (title?.startsWith("Screen_") == false) {
            binding.edtContent.setText(title)
        }
        binding.edtContent.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = binding.edtContent.text.toString()
                if (text.trim().isBlank()) {
                    TN.show(R.string.video_edit_title_empty)
                } else {
                    block?.invoke(text)
                    dismiss()
                }
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }

    override fun onStart() {
        super.onStart()
        KeyboardUtil.showSoftInput(binding.edtContent)
    }

    override fun dismiss() {
        KeyboardUtil.hideSoftInput(binding.edtContent)
        super.dismiss()
    }
}