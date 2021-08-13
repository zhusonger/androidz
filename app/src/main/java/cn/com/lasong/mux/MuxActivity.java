package cn.com.lasong.mux;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.com.lasong.app.AppBaseActivity;
import cn.com.lasong.base.result.PERCallback;
import cn.com.lasong.databinding.ActivityMuxBinding;
import cn.com.lasong.media.AVPixelFormat;
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
    ExecutorService executors = Executors.newSingleThreadExecutor();
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

                        try {
//                            File file = new File(Environment.getExternalStorageDirectory(), "geqian.mp3");
//                            File out = new File(Environment.getExternalStorageDirectory(), "geqian2.mp3");

                            File file = new File(Environment.getExternalStorageDirectory(), "test.mp4");
                            File out = new File(Environment.getExternalStorageDirectory(), "test2.mp4");
                            if (out.exists()) {
                                out.delete();
                            }
                            int ret = Muxer.remux(file.getAbsolutePath(), out.getAbsolutePath(), 4.8, 0);
                            ILog.d("ret: " + ret);
                            if (ret == 0) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(out.getAbsolutePath()), "video/mp4");
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
        binding.videoMuxing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new PERCallback() {
                    @Override
                    public void onResult(boolean isGrant, Map<String, Boolean> result) {
                        if (!isGrant) {
                            return;
                        }
                        executors.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Muxer muxer = new Muxer();
                                    InputStream is = getAssets().open("rgba_7.raw");
                                    InputStream is2 = getAssets().open("rgba_8.raw");
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    byte[] buffer = new byte[2048];
                                    int len;
                                    while ((len = is.read(buffer)) > 0) {
                                        bos.write(buffer, 0, len);
                                    }
                                    byte[] rgba1 = bos.toByteArray();
                                    bos.reset();
                                    while ((len = is2.read(buffer)) > 0) {
                                        bos.write(buffer, 0, len);
                                    }
                                    byte[] rgba2 = bos.toByteArray();
                                    bos.close();
                                    is.close();
                                    is2.close();
                                    File out = new File(Environment.getExternalStorageDirectory(), "muxing.mp4");
                                    if (out.exists()) {
                                        out.delete();
                                    }
                                    int ret = 0;
                                    long handle = muxer.init(out.getAbsolutePath());
                                    ret = muxer.add_video_stream(800_000, 360, 360);
                                    ret = muxer.scale_video(AVPixelFormat.AV_PIX_FMT_RGBA.ordinal(), 512, 512);
                                    ret = muxer.start();

                                    int allFrameCount = 30 * 10; // 10s
                                    int index = 0;
                                    byte[] rgba = rgba1;
                                    while (index <= allFrameCount) {
                                        if (index % 30 == 0) {
                                            if (rgba == rgba1) {
                                                rgba = rgba2;
                                            } else {
                                                rgba = rgba1;
                                            }
                                        }
                                        ret = muxer.write_video_frame(rgba);
                                        index++;
                                    }
                                    ret = muxer.write_video_frame(null);
                                    ret = muxer.stop();

                                    ILog.d("ret: " + ret);
                                    if (ret == 0) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setDataAndType(Uri.parse(out.getAbsolutePath()), "video/mp4");
                                                startActivity(intent);
                                            }
                                        });

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
    }
}
