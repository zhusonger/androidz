package cn.com.lasong.widget.text;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/08/12
 * Description: 可控制图标大小的文本控件
 */

public class ResizeEditText extends AppCompatEditText {
    public ResizeEditText(Context context) {
        this(context, null);
    }

    public ResizeEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public ResizeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DrawableResizeHelper.resizeDrawable(context, attrs, this);
    }
}
