package cn.com.lasong.zapp.base

import android.R
import android.view.View
import android.view.ViewGroup
import cn.com.lasong.base.BaseActivity
import cn.com.lasong.widget.utils.ViewHelper

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/5
 * Description: 应用类基础activity
 */
open class AppBaseActivity : BaseActivity() {
    /**
     * 适配状态栏类型枚举
     */
    enum class FitStatusBarType {
        NONE,  // 没有适配
        PADDING,  // 以padding适配
        MARGIN // 以margin适配
    }

    /**
     * 设置完布局后的回调
     */
    override fun onContentChanged() {
        super.onContentChanged()
        // 透明化状态栏
        if (transparentStatusBar()) {
            ViewHelper.transparentStatusBar(this)
        }
        // 适配状态栏
        if (transparentStatusBar() && fitStatusBarType() != FitStatusBarType.NONE) {
            val content = findViewById<View>(R.id.content)
            if (content is ViewGroup) {
                val resId = fitStatusBarResId()
                val appContent = if (resId == View.NO_ID) {
                    content.getChildAt(0)
                } else {
                    content.findViewById(resId)
                }
                ViewHelper.fitStatusBar(appContent, fitStatusBarType() == FitStatusBarType.PADDING)
            }
        }
    }

    /**
     * 适配透明化状态栏的方式
     * 默认使用PADDING方式
     * @return
     */
    open fun fitStatusBarType(): FitStatusBarType {
        return FitStatusBarType.PADDING
    }

    /**
     * 返回 需要适配透明化状态栏的控件ID
     * NO_ID 代表当前activity布局的ROOT控件
     * @return
     */
    open fun fitStatusBarResId(): Int {
        return View.NO_ID
    }

    /**
     * 是否透明化状态栏
     * @return
     */
    open fun transparentStatusBar(): Boolean {
        return false
    }
}