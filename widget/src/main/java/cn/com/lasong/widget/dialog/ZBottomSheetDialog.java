package cn.com.lasong.widget.dialog;

import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/3
 * Description:
 */
public class ZBottomSheetDialog extends BottomSheetDialog {
    public ZBottomSheetDialog(@NonNull Context context) {
        super(context, R.style.ZBottomSheetDialog);
    }
}
