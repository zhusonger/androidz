package cn.com.lasong.widget.adapterview.loadmore;

public interface LoadMoreUIHandler {

    void onLoading(LmRecyclerView container);

    void onLoadFinish(LmRecyclerView container, boolean empty, boolean hasMore);

    void onWaitToLoadMore(LmRecyclerView container);

    void onLoadError(LmRecyclerView container, int errorCode, String errorMessage);
}