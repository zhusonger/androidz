package cn.com.lasong.widget.adapterview.loadmore;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import cn.com.lasong.widget.adapterview.adapter.Bookends;


/**
 * Created by zhusong on 17/5/9.
 * load more recycler view
 */
@SuppressWarnings("unused")
public class LmRecyclerView extends RecyclerView {

    private final String TAG = "LmRecyclerView";

    private Bookends<?> mAdapter;
    private LoadMoreUIHandler mLoadMoreUIHandler;
    private LoadMoreHandler mLoadMoreHandler;

    private boolean mIsLoading;
    private boolean mHasMore = false;
    private boolean mAutoLoadMore = true;
    private boolean mLoadError = false;

    private boolean mListEmpty = true;
    private boolean mShowLoadingForFirstPage = false;
    private View mFooterView;
    private int mFooterTextColor;

    private boolean mEnable = false;

    private boolean debug = false;

    public LmRecyclerView(Context context) {
        this(context, null);
    }

    public LmRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LmRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mFooterTextColor = Color.parseColor("#b3ffffff");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void useDefaultFooter() {
        mEnable = true;
        LoadMoreDefaultFooterView footerView = new LoadMoreDefaultFooterView(getContext());
        footerView.setVisibility(GONE);
        setLoadMoreView(footerView);
        setLoadMoreUIHandler(footerView);
        footerView.setFooterTextColor(mFooterTextColor);
    }

    public void enableLoadMore() {
        mEnable = true;
    }

    public void disableLoadMore() {
        mEnable = false;
    }

    private void init() {
        addOnScrollListener(new ReachEndListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (!mEnable) {
                    if (debug) Log.w(TAG, "LmRecyclerView is not enable");
                    return;
                }
                if (mIsLoading) {
                    if (debug) Log.w(TAG, "LmRecyclerView is loading");
                    return;
                }

                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            protected void onReachBottom() {
                LmRecyclerView.this.onReachBottom();
            }
        });

    }

    private void tryToPerformLoadMore() {
        if (mIsLoading) {
            return;
        }

        // no more content and also not load for first page
        if (!mHasMore && !(mListEmpty && mShowLoadingForFirstPage)) {
            return;
        }

        mIsLoading = true;

        if (mLoadMoreUIHandler != null) {
            mLoadMoreUIHandler.onLoading(this);
        }
        if (null != mLoadMoreHandler) {
            mLoadMoreHandler.onLoadMore(this);
        }
    }

    private void onReachBottom() {
        // if has error, just leave what it should be
        if (mLoadError) {
            return;
        }
        if (mAutoLoadMore) {
            tryToPerformLoadMore();
        } else {
            if (mHasMore) {
                mLoadMoreUIHandler.onWaitToLoadMore(this);
            }
        }
    }

    public void setShowLoadingForFirstPage(boolean showLoading) {
        mShowLoadingForFirstPage = showLoading;
    }

    public void setAutoLoadMore(boolean autoLoadMore) {
        mAutoLoadMore = autoLoadMore;
    }

    public void setLoadMoreView(@NonNull View view) {
        if (null == mAdapter) {
            throw new IllegalStateException("must setLmAdapter first");
        }
        // remove previous
        if (mFooterView != null && mFooterView != view) {
            mAdapter.removeFooter(mFooterView);
        }

        // add current
        mFooterView = view;
        mFooterView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToPerformLoadMore();
            }
        });

        mAdapter.addFooter(view);
    }

    public void setLoadMoreUIHandler(LoadMoreUIHandler handler) {
        mLoadMoreUIHandler = handler;
    }

    public void setLoadMoreHandler(LoadMoreHandler handler) {
        mLoadMoreHandler = handler;
    }

    /**
     * page has loaded
     *
     * @param emptyResult
     * @param hasMore
     */
    public void loadMoreFinish(boolean emptyResult, boolean hasMore) {
        mLoadError = false;
        mListEmpty = emptyResult;
        mIsLoading = false;
        mHasMore = hasMore;

        if (mLoadMoreUIHandler != null) {
            if (hasMore && !mAutoLoadMore) {
                mLoadMoreUIHandler.onWaitToLoadMore(this);
            } else {
                mLoadMoreUIHandler.onLoadFinish(this, emptyResult, hasMore);
            }
        }
    }

    public void loadMoreError(int errorCode, String errorMessage) {
        mIsLoading = false;
        mLoadError = true;
        if (mLoadMoreUIHandler != null) {
            mLoadMoreUIHandler.onLoadError(this, errorCode, errorMessage);
        }
    }


    public void addHeader(View view) {
        if (null == mAdapter) {
            throw new IllegalStateException("must setLmAdapter first");
        }
        mAdapter.addHeader(view);
    }

    public void addFooter(View view) {
        if (null == mAdapter) {
            throw new IllegalStateException("must setLmAdapter first");
        }

        if (null != mFooterView) {
            throw new IllegalStateException("addFooter must call before setLoadMoreView");
        }
        mAdapter.addFooter(view);
    }

    public <T extends RecyclerView.Adapter> void setLmAdapter(T adapter) {
        if (null != mAdapter) {
            RecyclerView.Adapter<?> wrapAdapter = mAdapter.getWrappedAdapter();
            if (null != wrapAdapter) {
                wrapAdapter.unregisterAdapterDataObserver(mDataObserver);
            }
            mAdapter.onDetachedFromRecyclerView(this);
        }
        if (null != adapter) {
            mAdapter = new Bookends<>(adapter);
            adapter.registerAdapterDataObserver(mDataObserver);
            super.setAdapter(mAdapter);
        }
    }

    @Override
    @Deprecated
    public void setAdapter(Adapter adapter) {
        if (isInEditMode()) {
            super.setAdapter(adapter);
        } else {
            throw new IllegalArgumentException("please call setLmAdapter");
        }
    }


    private AdapterDataObserver mDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (null != mAdapter) {
                if (debug) Log.d(TAG, "onChanged");
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (debug) Log.d(TAG, "onItemRangeChanged : " + positionStart + "," + itemCount);
            if (null != mAdapter) {
                mAdapter.notifyItemChanged(positionStart + mAdapter.getHeaderCount(), itemCount);
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (debug) Log.d(TAG, "onItemRangeChanged : " + positionStart + "," + itemCount + "," + payload);
            if (null != mAdapter) {
                mAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (debug) Log.d(TAG, "onItemRangeInserted : " + positionStart + "," + itemCount);
            if (null != mAdapter) {
                mAdapter.notifyItemRangeInserted(positionStart + mAdapter.getHeaderCount(), itemCount);
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (debug) Log.d(TAG, "onItemRangeRemoved : " + positionStart + "," + itemCount);
            if (null != mAdapter) {
                mAdapter.notifyItemRangeRemoved(positionStart + mAdapter.getHeaderCount(), itemCount);
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (debug) Log.d(TAG, "onItemRangeMoved : " + fromPosition + "," + toPosition);
            if (null != mAdapter) {
                mAdapter.notifyItemMoved(fromPosition, toPosition);
            }
        }
    };
}
