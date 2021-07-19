package cn.com.lasong.zapp.ui.all

import android.content.Context
import android.os.Bundle
import android.widget.CheckedTextView
import androidx.recyclerview.widget.LinearLayoutManager
import cn.com.lasong.widget.adapterview.adapter.OnItemClickListener
import cn.com.lasong.widget.adapterview.adapter.ZRecyclerViewAdapter
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.databinding.DialogOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/6
 * Description:
 * 选项弹窗
 */
class OptionDialog(context: Context) : BottomSheetDialog(context, R.style.ZBottomSheetDialog) {

    companion object {
        fun newInstance(
            context: Context,
            title: String,
            options: Array<String>,
            selectedIndex: Int = 0,
            listener: OnItemClickListener? = null,
        ): OptionDialog {
            val dialog = OptionDialog(context)
            dialog.title = title
            dialog.options = options
            dialog.selectedIndex = selectedIndex
            dialog.listener = listener
            return dialog
        }
    }

    lateinit var title: String
    lateinit var options: Array<String>
    var selectedIndex: Int = 0
    var listener: OnItemClickListener? = null

    lateinit var adapter: ZRecyclerViewAdapter<String>
    val data : MutableList<String> = ArrayList()

    lateinit var binding: DialogOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        data.addAll(options)
        adapter = object : ZRecyclerViewAdapter<String>(data, R.layout.item_option,
            OnItemClickListener { view, position ->
                listener?.onItemClick(view, position)
                dismiss()
            }) {
            override fun bind(holder: AdapterViewHolder, item: String?, position: Int) {
                val textView = holder.itemView as CheckedTextView
                textView.text = item
                textView.isChecked = position == selectedIndex
            }
        }

        binding.rvOptions.layoutManager = LinearLayoutManager(context)
        binding.rvOptions.adapter = adapter
        binding.tvTitle.text = title
    }
}