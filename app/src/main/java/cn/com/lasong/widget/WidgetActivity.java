package cn.com.lasong.widget;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import cn.com.lasong.R;
import cn.com.lasong.app.AppBaseActivity;
import cn.com.lasong.databinding.ActivityWidgetBinding;
import cn.com.lasong.widget.dialog.TopSheetDialog;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/12
 * Description:
 */
public class WidgetActivity extends AppBaseActivity {

    private ActivityWidgetBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWidgetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvText.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (binding.tvText.getVisibility() == View.VISIBLE) {
                    binding.tvText.setVisibility(View.GONE);
                } else {
                    binding.tvText.setVisibility(View.VISIBLE);
                }

                binding.tvText.postDelayed(this, 2000);
            }
        }, 2000);
    }

    @Override
    protected int fitStatusBarResId() {
        return R.id.ll_content;
    }

    @Override
    protected FitStatusBarType fitStatusBarType() {
        return FitStatusBarType.MARGIN;
    }

    private TopSheetDialog dialog;
    private IconMoveView moveView;
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_top_sheet: {
                if (null != dialog)
                    dialog.dismiss();
                dialog = new TopSheetDialog(this);
                dialog.setContentView(R.layout.view_lyric);
                // 可隐藏
                dialog.setCancelable(true);
                // 是否拦截触摸事件
                dialog.setConsumeTouch(false);
                // 在控件外触摸不隐藏
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                break;
            }

            case R.id.btn_move: {
                if (null != moveView) {
                    if (moveView.getVisibility() == View.VISIBLE) {
                        moveView.hide();
                        binding.btnMove.setText("显示浮窗");
                    } else {
                        moveView.show(this);
                        binding.btnMove.setText("隐藏浮窗");
                    }
                } else {
                    moveView = new IconMoveView(this);
                    moveView.show(this);
                    binding.btnMove.setText("隐藏浮窗");
                }
                break;
            }
        }
    }
}
