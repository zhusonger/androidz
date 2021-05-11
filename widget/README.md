# Widget
常用控件集合

# 引入

```
implementation 'com.github.zhusonger.androidz:widget:1.0.0'
```

## 1.0.0

* 添加歌词控件LrcView  
使用TextureView实现歌词渲染, 提高渲染效率  
![](https://www.lasong.com.cn/assets/img/gif/lyric.gif)

* 添加拖动控件MoveView
 使用相对布局实现移动布局, 可以像普通RelativeLayout一样使用, 支持移动被MoveView包含的所有内容   
 ![](https://www.lasong.com.cn/assets/img/gif/move.gif)

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

* 优化移动控件
* 处理全屏状态栏的间距
* 添加富文本控件
* 添加ZTabLayout, 支持结合viewpager使用自定义布局
* AdapterAlertDialog支持x, y偏移

## 1.0.1-alpha0

* 处理1.3.0TabLayout长按的tooltip提醒

## 1.0.1-alpha1

* 新增阴影控件v2, 自适应内容, 并修复列表中背景不显示的问题

    ```java
    cn.com.lasong.widget.shadow.v2.ShadowLayout
    ```

## 1.0.1-alpha2

* 修复阴影控件背景范围显示不正确的问题
* 新增背景圆角控件

    ```java
    cn.com.lasong.widget.RadiusLayout
    ```

## 1.0.1-alpha3

* 新增RecyclerView吸顶分组实现,

    ```java
    cn.com.lasong.widget.adapterview.itemdecoration.StickyHeaderDecoration
    cn.com.lasong.widget.adapterview.itemdecoration.StickHeaderProvider

    // 使用方法
    StickyHeaderDecoration stickyHeaderDecoration = new StickyHeaderDecoration();
    // 重写adapter的onAttachedToRecyclerView, 进行捆绑
    stickyHeaderDecoration.attachRecyclerView(recyclerView, <StickHeaderProvider实现>);
    // 重写adapter的onDetachedFromRecyclerView, 进行释放
    stickyHeaderDecoration.detachRecyclerView(recyclerView);

    // StickHeaderProvider的实现是2个方法
    // createOrUpdateHeader 主要是创建吸顶的布局
    // isStickHeader判断位置是否是吸顶布局
    @Override
    public RecyclerView.ViewHolder createOrUpdateHeader(RecyclerView.ViewHolder cache, LayoutInflater inflater, RecyclerView parent, int position) {
        if (!isStickHeader(position)) {
            return null;
        }
        ViewHolder holder = cache;
        if (null == holder) {
            View v = inflater.inflate(R.layout.item_sticker_header, parent, false);
            holder = new ZRecyclerViewAdapter.AdapterViewHolder(v, null, null);
        }

        adapter.bind((ZRecyclerViewAdapter.AdapterViewHolder) holder, data.get(position), position);
        return holder;
    }

    @Override
    public boolean isStickHeader(int position) {
        return position % 10 == 0;
    }
    ```

## 1.0.1-alpha4

* 扩展RadiusLayout, 添加边线