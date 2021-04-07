package cn.com.lasong.widget.adapterview.loadmore;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.com.lasong.widget.R;


/**
 * Created by zhusong on 17/5/9.
 */

public class LoadMoreDefaultFooterView extends RelativeLayout implements LoadMoreUIHandler {

    private TextView mTv;
    private ProgressBar mPb;
    public LoadMoreDefaultFooterView(Context context) {
        super(context);
        init();
    }

    public LoadMoreDefaultFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadMoreDefaultFooterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_load_more_default_footer, this);
        mTv = (TextView) findViewById(R.id.tv);
        mPb = (ProgressBar) findViewById(R.id.pb);
    }

    @Override
    public void onLoading(LmRecyclerView container) {
        setVisibility(View.VISIBLE);
        mTv.setText(R.string.load_more_loading);
        mPb.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoadFinish(LmRecyclerView container, boolean empty, boolean hasMore) {
        setVisibility(GONE);
    }

    @Override
    public void onWaitToLoadMore(LmRecyclerView container) {
        setVisibility(View.VISIBLE);
        mTv.setText(R.string.load_more_wait_to_load);
        mPb.setVisibility(View.GONE);
    }

    @Override
    public void onLoadError(LmRecyclerView container, int errorCode, String errorMessage) {
        setVisibility(View.VISIBLE);
        mTv.setText(errorMessage);
        mPb.setVisibility(View.GONE);
    }

    public void setFooterTextColor(int color) {
        mTv.setTextColor(color);
    }
}
