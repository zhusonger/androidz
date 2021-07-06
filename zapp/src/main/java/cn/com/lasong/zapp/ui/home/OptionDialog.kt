package cn.com.lasong.zapp.ui.home

import android.content.Context
import android.os.Bundle
import cn.com.lasong.zapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/6
 * Description:
 */
class OptionDialog(context: Context) : BottomSheetDialog(context, R.style.ZBottomSheetDialog) {

    companion object {
        fun newInstance(context: Context, title: String, options: Array<String>): OptionDialog {
            val dialog = OptionDialog(context)
            dialog.title = title
            dialog.options = options
            return dialog
        }
    }

    lateinit var title: String
    lateinit var options: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_options)
    }

}