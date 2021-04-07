# Widget
常用控件集合

# 引入

```
implementation 'cn.com.lasong:widget:latest.release'
```

## v0.0.1  

* 添加歌词控件LrcView  
使用TextureView实现歌词渲染, 提高渲染效率  
![](https://www.lasong.com.cn/assets/img/gif/lyric.gif)

* 添加拖动控件MoveView
 使用相对布局实现移动布局, 可以像普通RelativeLayout一样使用, 支持移动被MoveView包含的所有内容   
 ![](https://www.lasong.com.cn/assets/img/gif/move.gif)

## v0.0.2

* 添加可设置图标大小的CheckedTextView&EditText

    ```xml
    <cn.com.lasong.widget.text.ResizeTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:drawableLeft="@drawable/ic_widget"
        app:drawLeftWidth="32dp"
        app:drawLeftHeight="32dp"
        android:text="自定义TextView图标大小"
        android:gravity="center"
        android:drawablePadding="10dp"
        android:padding="5dp"
        android:textSize="16sp"
        />

    <cn.com.lasong.widget.text.ResizeEditText
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:drawableLeft="@drawable/ic_widget"
        app:drawLeftWidth="32dp"
        app:drawLeftHeight="32dp"
        android:hint="自定义EditText图标大小"
        android:gravity="center"
        android:drawablePadding="10dp"
        android:background="@null"
        android:padding="5dp"
        android:textSize="16sp"
        />
    ```

## v0.0.3

* 添加阴影控件, 自定义背景色与阴影色, 阴影控件与实际展示内容大小一致, 不需要预留阴影的空间

    ```xml
    <cn.com.lasong.widget.shadow.ShadowLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        app:bgColor="@android:color/white"
        app:bgRadius="10dp"
        app:shadowColor="#4D000000"
        app:shadowRadius="4dp"
        app:shadowDx="3dp"
        app:shadowDy="3dp"
        >

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="10dp">


            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:minWidth="150dp"
                android:minHeight="40dp"
                android:text="阴影控件内容"
                android:textSize="20sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:minWidth="150dp"
                android:minHeight="40dp"
                android:text="阴影控件内容" />
        </LinearLayout>
    </cn.com.lasong.widget.shadow.ShadowLayout>
    ```

## v0.0.4

* 添加顶部滑入弹窗

    ```java
    // 可使用自定义theme关闭dim
    TopSheetDialog dialog = new TopSheetDialog(this);
    dialog.setContentView(R.layout.x);
    // 是否拦截触摸事件, 不拦截发现触摸偏移的话, 检查使用的主题刘海屏是否是shortEdges/never
    dialog.setConsumeTouch(false);
    // 在控件外触摸不隐藏
    dialog.setCanceledOnTouchOutside(false);
    // 可隐藏
    dialog.setCancelable(true);
    dialog.show();
    ```

## v0.0.5

* 添加AdapterAlertDialog, 简化弹窗的实现

    ```java
    public class MyDialog extends AdapterAlertDialog {
        public MyDialog(Context context) {
            super(context);
        }

        @Override
        protected View getView(LayoutInflater inflater, ViewGroup parent) {
            return null;
        }

        @Override
        protected boolean showSoftInput() {
            // 是否需要输入, 默认不需要
            return super.showSoftInput();
        }

        @Override
        protected Rect getWindowSize(int screenWidth, int screenHeight) {
            // 获取大小, 默认自适应
            return super.getWindowSize(screenWidth, screenHeight);
        }

        @Override
        protected int getGravity() {
            // 获取位置, 默认居中
            return super.getGravity();
        }
    }
    ```

* 添加ZRecyclerViewAdapter适配器, 简化RecycleView的适配器创建类的过程

    ```java
    List<String> data = new ArrayList<>();
    ZRecyclerViewAdapter<String> adapter = new ZRecyclerViewAdapter<String>(data, R.layout.item_view) {
        @Override
        public void bind(AdapterViewHolder holder, String item, int position) {

        }
    };

    data.add("Item0");
    data.add("Item1");
    adapter.notifyItemRangeChanged(0, 2);
    ```

* 添加LmRecyclerView, 简化加载更多的处理

    ```java
    LmRecyclerView lmRecyclerView = findViewById(R.id.rv_lm);
    ZRecyclerViewAdapter<String> adapter = new ZRecyclerViewAdapter<String>(data, R.layout.item_view) {
        @Override
        public void bind(AdapterViewHolder holder, String item, int position) {

        }
    };
    lmRecyclerView.setLoadMoreHandler(new LoadMoreHandler() {
        @Override
        public void onLoadMore(LmRecyclerView loadMoreContainer) {
            // 实现加载更多逻辑
            // 根据加载结果反馈给LmRecyclerView
            loadMoreContainer.loadMoreFinish(/*emptyResult*/false, /*hasMore*/false);
        }
    });
    lmRecyclerView.setLmAdapter(adapter);
    // 开启加载更多功能, 默认关闭
    lmRecyclerView.enableLoadMore();
    // 关闭加载更多功能
    lmRecyclerView.disableLoadMore();
    ```

    ### 0.0.5.2
    * 优化移动控件
    * 处理全屏状态栏的间距
    * 添加富文本控件

    ### 0.0.5.3
    * 添加ZTabLayout, 支持结合viewpager使用自定义布局

    ### 0.0.5.4
        * AdapterAlertDialog支持x, y偏移
