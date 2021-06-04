package cn.com.lasong.mux;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Map;

import cn.com.lasong.app.AppBaseActivity;
import cn.com.lasong.base.result.PERCallback;
import cn.com.lasong.databinding.ActivityMuxBinding;
import cn.com.lasong.media.Muxer;
import cn.com.lasong.utils.ILog;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/2
 * Description:
 */
public class MuxActivity extends AppBaseActivity {

    ActivityMuxBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMuxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.mux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new PERCallback() {
                    @Override
                    public void onResult(boolean isGrant, Map<String, Boolean> result) {
                        if (!isGrant) {
                            return;
                        }
                        Muxer muxer = new Muxer();

                        try {
//                            File file = new File(Environment.getExternalStorageDirectory(), "geqian.mp3");
//                            File out = new File(Environment.getExternalStorageDirectory(), "geqian2.mp3");

                            File file = new File(Environment.getExternalStorageDirectory(), "test.mp4");
                            File out = new File(Environment.getExternalStorageDirectory(), "test2.mp4");
                            if (out.exists()) {
                                out.delete();
                            }
                            int ret = muxer.remux(file.getAbsolutePath(), out.getAbsolutePath(), 1, 6.8);
                            ILog.d("ret: " + ret);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
    }
}
