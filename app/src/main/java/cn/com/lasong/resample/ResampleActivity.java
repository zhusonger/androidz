package cn.com.lasong.resample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import cn.com.lasong.R;
import cn.com.lasong.app.AppBaseActivity;
import cn.com.lasong.base.result.PERCallback;
import cn.com.lasong.media.AVChannelLayout;
import cn.com.lasong.media.AVSampleFormat;
import cn.com.lasong.media.Resample;
import cn.com.lasong.utils.ILog;

public class ResampleActivity extends AppBaseActivity implements View.OnClickListener {

    private boolean mIsRunning = false;

    private EditText mOutputEt;
    private Spinner mOutSampleRatesSp;
    private Spinner mOutChannelLayoutsSp;
    private Spinner mOutSampleFmtsSp;
    private Button mConvertBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resample);
        mOutputEt = findViewById(R.id.output_et);
        mOutSampleRatesSp = findViewById(R.id.out_sample_rates_sp);
        mOutChannelLayoutsSp = findViewById(R.id.out_channel_layouts_sp);
        mOutSampleFmtsSp = findViewById(R.id.out_sample_fmts_sp);
        mConvertBtn = findViewById(R.id.convert_btn);

        File file = new File(Environment.getExternalStorageDirectory(),"/resample.pcm");
        mOutputEt.setText(file.getAbsolutePath());
        mOutSampleRatesSp.setSelection(5);
        mOutChannelLayoutsSp.setSelection(0);
        mOutSampleFmtsSp.setSelection(1);
        mConvertBtn.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        if (mIsRunning || TextUtils.isEmpty(mOutputEt.getText().toString())) {
            return;
        }

        requestPermissions(new PERCallback() {
            @Override
            public void onResult(boolean isGrant, Map<String, Boolean> result) {
                if (!isGrant) {
                    return;
                }
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        if (mIsRunning) {
                            return;
                        }
                        mIsRunning = true;
                        Resample resample = new Resample();
                        try {
                            String[] array = getResources().getStringArray(R.array.sample_rates);
                            resample.init(AVChannelLayout.AV_CH_LAYOUT_STEREO, AVSampleFormat.AV_SAMPLE_FMT_S16.ordinal(), 44100,
                                    mOutChannelLayoutsSp.getSelectedItemPosition() == 0 ? AVChannelLayout.AV_CH_LAYOUT_MONO : AVChannelLayout.AV_CH_LAYOUT_STEREO,
                                    mOutSampleFmtsSp.getSelectedItemPosition(), Integer.parseInt(array[mOutSampleRatesSp.getSelectedItemPosition()]));
                            InputStream is = getResources().getAssets().open("shimian.pcm");

                            File file = new File(mOutputEt.getText().toString());
                            if (file.exists()) {
                                file.delete();
                            }
                            file.createNewFile();

                            FileOutputStream fos = new FileOutputStream(file);
                            byte[] buf = new byte[2048];
                            byte[] out = new byte[2048];
                            final long start = System.nanoTime() / 1000_000;
                            int length = 0;
                            while ((length = is.read(buf)) > 0) {
                                int size = resample.resample(buf, length);
                                int read_size = resample.read(out, size);
                                fos.write(out, 0, read_size);
//                        Log.e("NDK_LOG", "SIZE = " + size+", READ SIZE = " + read_size);
                            }
                            final long end = System.nanoTime() / 1000_000;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ResampleActivity.this, "Resample Finish("+(end - start)+"ms)", Toast.LENGTH_SHORT).show();
                                    ILog.d("Resample Finish("+(end - start)+"ms)");
                                }
                            });
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            resample.release();
                        }
                        mIsRunning = false;
                    }
                }.start();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
    }
}
