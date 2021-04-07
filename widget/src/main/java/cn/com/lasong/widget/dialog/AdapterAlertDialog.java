package cn.com.lasong.widget.dialog;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import cn.com.lasong.widget.R;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/1/29
 * Description:
 */
public abstract class AdapterAlertDialog extends AlertDialog {
    public AdapterAlertDialog(Context context) {
        super(context, R.style.AdapterAlertDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public AdapterAlertDialog(Context context, int dialogStyle) {
        super(context, dialogStyle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        ViewGroup parent = null;
        if (null != window) {
            View decorView = window.getDecorView();
            parent = decorView.findViewById(android.R.id.content);
        }
        View view = getView(LayoutInflater.from(getContext()), parent);
        if (null != view) {
            setContentView(view);
        }
    }
    protected abstract View getView(LayoutInflater inflater, ViewGroup parent);
    protected boolean showSoftInput() {
        return false;
    }
    protected Rect getWindowSize(int screenWidth, int screenHeight) {
        return null;
    }

    protected int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            Rect size = getWindowSize(dm.widthPixels, dm.heightPixels);
            if (size == null) {
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                window.setLayout(
                        size.width() > 0 ? size.width() : ViewGroup.LayoutParams.WRAP_CONTENT,
                        size.height() > 0 ? size.height() : ViewGroup.LayoutParams.WRAP_CONTENT
                );
                int yOffset = size.top;
                int xOffset = size.left;
                WindowManager.LayoutParams params = window.getAttributes();
                params.y = yOffset;
                params.x = xOffset;
                window.setAttributes(params);
            }

            if (showSoftInput()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
            window.setGravity(getGravity());
        }


    }
}
