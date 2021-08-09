package cn.com.lasong.gallery;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import cn.com.lasong.R;
import cn.com.lasong.app.AppBaseActivity;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description:
 */
public class GalleryActivity extends AppBaseActivity implements View.OnClickListener {

    private ImageView mIvIcon;
//    private Transferee transfer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
//        transfer = Transferee.getDefault(this);
        mIvIcon = findViewById(R.id.iv_icon);
        mIvIcon.setOnClickListener(this);
        // 设置默认图
        Glide.with(this).load(SourceConfig.getThumbSourceGroup().get(0)).into(mIvIcon);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        transfer.destroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mIvIcon) {
//            TransferConfig config = TransferConfig.build()
//                    .setSourceUrlList(SourceConfig.getThumbSourceGroup())
//                    .setProgressIndicator(new ProgressPieIndicator())
//                    .setImageLoader(GlideImageLoader.with(getApplicationContext()))
//                    .enableHideThumb(false)
//                    .bindImageView(mIvIcon);
//
//            config.setNowThumbnailIndex(0);
//            transfer.apply(config).show();
        }
    }
}
