package cn.com.lasong.widget.lyric;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Region;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/03/04
 * Description: 歌词控件
 */
public class LrcView extends TextureView implements TextureView.SurfaceTextureListener, ITimeProvider, Runnable {

    public LrcView(Context context) {
        this(context, null);
    }

    public LrcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private boolean mDestroy = false;
    private final Object mFence = new Object();
    private Thread mThread;
    // =====Draw=======//
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 不在播放中的字体大小
    private final int DEFAULT_TEXT_SIZE_PX = sp2px(16);
    // 播放中的字体大小
    private final int PLAYING_TEXT_SIZE_PX = sp2px(18);
    // 不在播放中的字体颜色
    private final int DEFAULT_TEXT_COLOR = Color.parseColor("#99FFFFFF");
    // 当前正在播放行的颜色
    private final int PLAYING_DEFAULT_TEXT_COLOR = Color.WHITE;
    // 已经播放过的文字的颜色
    private final int PLAYING_PASS_TEXT_COLOR = Color.parseColor("#FF479A");
    private final int ROW = 3;

    // time provider
    private ITimeProvider mProvider;

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 设置透明背景
        // 需要在能获取到Canvas后再次执行unlockCanvasAndPost解决切换窗口之后窗口变默认黑色的问题(onSurfaceTextureAvailable)
        setOpaque(false);
        setSurfaceTextureListener(this);
        mPaint.setTextSize(DEFAULT_TEXT_SIZE_PX);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(DEFAULT_TEXT_COLOR);
        mThread = new Thread(this, "LrcView");
        mThread.start();
    }

    /**
     * Value of sp to value of px.
     *
     * @param spValue The value of sp.
     * @return value of px
     */
    int sp2px(final float spValue) {
        final float fontScale = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mDestroy = false;
        Canvas canvas = lockCanvas();
        if (null != canvas) {
            mPaint.setTextSize(DEFAULT_TEXT_SIZE_PX);
            mPaint.setColor(DEFAULT_TEXT_COLOR);
            // 计算垂直居中的基线
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            float centerY = mHeight / 2;
            float centerX = mWidth / 2;
            int baselineY = (int) (centerY - (fontMetrics.top + fontMetrics.bottom) / 2);
            // 画默认歌词样式
            canvas.drawText("Waiting For You!", centerX, baselineY, mPaint);

            canvas.drawColor(Color.TRANSPARENT);
        }
        unlockCanvasAndPost(canvas);
        synchronized (mFence) {
            mFence.notifyAll();
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mDestroy = true;
        synchronized (mFence) {
            mFence.notifyAll();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDestroy = true;
        synchronized (mFence) {
            mFence.notifyAll();
        }
    }

    /**
     * 设置时间提供器
     * @param provider
     */
    public void setProvider(ITimeProvider provider) {
        this.mProvider = provider;
    }

    /**
     * 开始显示歌词
     * @param path
     * @return
     */
    public boolean showLyric(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            return showLyric(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean showLyric(InputStream in) {
        Lyric lrc = LyricUtils.readLyric(in);
        return showLyric(lrc);
    }

    private Lyric mLrc;
    // 开始渲染时间
    private long mStartRenderTs;
    public boolean showLyric(Lyric lrc) {
        if (null == lrc) {
            return false;
        }
        mLrc = lrc;
        mStartRenderTs = System.currentTimeMillis();
        // 最后设置为可见, 解锁
        setVisibility(View.VISIBLE);
        return true;
    }

    // 控件宽高
    private float mWidth;
    private float mHeight;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // 显示通知解锁
        if (visibility == View.VISIBLE) {
            synchronized (mFence) {
                mFence.notifyAll();
            }
        }
    }

    /**
     * 判断是否需要阻塞线程, 避免无意义的循环画歌词
     * 避免CPU消耗
     * @return
     */
    private boolean isBlock() {
        // surface未准备好
        // 当前视图不可见
        // 当前歌词未变化
        // 歌词未开始/没有歌词
        boolean isUnAvailable = !isAvailable();
        boolean isUnVisible = getVisibility() != View.VISIBLE;
        boolean isInValidLrc = mLrc == null || null == mLrc.lines || mLrc.lines.size() == 0;
        boolean isPlayEnd = null != mLrc && getCurrentPosition() > mLrc.total;

        return isUnAvailable || isUnVisible || isInValidLrc || isPlayEnd;
    }

    @Override
    public void run() {
        // 移除窗口后退出线程
        while (!mDestroy) {
            // 判断是否不需要画歌词
            if (isBlock()) {
                // 阻塞渲染歌词线程
                synchronized (mFence) {
                    try {
                        mFence.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 再次回到while循环判断
                // 1. 可能是view被移除释放的阻塞, mDetach = true, 线程结束
                // 2. 正常的视图可见/surface创建并且可见, 不会进入阻塞, 正常执行下面的渲染逻辑
                continue;
            }

            List<LyricLine> lines = mLrc.lines;
            if (null == lines || lines.size() <= 0) {
                continue;
            }

            long passMs = getCurrentPosition();
            int lineSize = lines.size();
            int current = 0;
            long offset = null != mLrc ? mLrc.offset : 0;
            // 查询当前时长播放到哪一行的歌词
            for (int i = 0; i < lineSize; i++) {
                LyricLine line = lines.get(i);
                long start = line.start + offset;
                long end = start + line.duration;
                // 当前正在播放行
                if (passMs >= start) {
                    current = i;
                }

                if (passMs < start) {
                    break;
                }
            }

            // 展示的歌词中间行位置
            int centerLine = ROW / 2;
            LyricLine curLrc = lines.get(current);
            LyricLine[] lrcArray = new LyricLine[ROW];
            for (int i = 0; i < ROW; i++) {
                // 与中间行对比, 得到偏差值
                int lrcOffset = centerLine - i;
                // 居中行前/居中行
                if (lrcOffset >= 0) {
                    lrcArray[i] = current - lrcOffset >= 0 ? lines.get(current - lrcOffset) : null;
                }
                // 居中行后
                else {
                    lrcArray[i] = current - lrcOffset < lineSize ? lines.get(current - lrcOffset) : null;
                }
            }
            // 计算歌词播放百分比
            float percent;
            long linePassMs = passMs - (curLrc.start + offset);
            // 不在歌词播放时长范围
            if (linePassMs < 0 || linePassMs > curLrc.duration) {
                linePassMs = linePassMs < 0 ? 0 : curLrc.duration;
                percent = linePassMs <= 0 ? 0f : 1.0f;
            }
            // 在歌词播放时长范围
            // 由于每个字时长不定, 所以通过对比每个歌词来得到百分比
            else {
                List<LyricWord> words = curLrc.words;
                int wordSize = null != words ? words.size() : 0;
                float passWords = 0;
                for (int i = 0; i < wordSize; i++) {
                    LyricWord word = words.get(i);
                    // 这个字已经播放结束
                    if (word.start + word.duration <= linePassMs) {
                        passWords++;
                    }
                    // 这个字正在播放 但是未播放结束
                    else if (linePassMs >= word.start){
                        passWords += (linePassMs - word.start) * 1.0f / word.duration;
                        break;
                    }
                }
                // 得到当前整行歌词对应的播放百分比
                percent = wordSize > 0 ? passWords / wordSize : 0;
            }


            Canvas canvas = lockCanvas();
            synchronized (mFence) {
                if (mDestroy) {
                    break;
                }
            }
            // 清除上一次的内容
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // 歌词
            final float centerX = mWidth / 2;
            float lineHeight = mHeight / ROW;

            float centerY = lineHeight / 2;
            for (int i = 0; i < ROW; i++) {
                String text = lrcArray[i] != null ? lrcArray[i].content : "";
                // 中间歌词白色高亮, 字体18sp
                if (centerLine == i) {
                    mPaint.setTextSize(PLAYING_TEXT_SIZE_PX);
                    mPaint.setColor(PLAYING_DEFAULT_TEXT_COLOR);
                } else {
                    mPaint.setTextSize(DEFAULT_TEXT_SIZE_PX);
                    mPaint.setColor(DEFAULT_TEXT_COLOR);
                }

                // 计算垂直居中的基线
                Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
                int baselineY = (int) (centerY - (fontMetrics.top + fontMetrics.bottom) / 2);
                // 画默认歌词样式
                canvas.drawText(text, centerX, baselineY, mPaint);

                // 正在播放的逐字高亮
                if (centerLine == i) {
                    final float textWidth = mPaint.measureText(text, 0, text.length());
                    final float textTop = centerY - lineHeight / 2;
                    final float textBottom = centerY + lineHeight / 2;
                    final float textLeft = (mWidth - textWidth) / 2;

                    // 通过裁切实现逐字高亮
                    canvas.save();
                    canvas.clipRect(textLeft, textTop, textLeft + textWidth, textBottom);
                    canvas.clipRect(textLeft, textTop, textLeft + textWidth * percent,
                            textBottom, Region.Op.INTERSECT);
                    mPaint.setColor(PLAYING_PASS_TEXT_COLOR);
                    canvas.drawText(text, centerX, baselineY, mPaint);
                    canvas.restore();
                }
                centerY += lineHeight;
            }
            unlockCanvasAndPost(canvas);
            // 间隔进行下一次绘制, 这个可以根据需要调整
            try {
                Thread.sleep(getPollingInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 隐藏歌词, 歌词不可显示后会阻塞歌词渲染, 释放资源
     */
    public void hideLrc() {
        setVisibility(View.GONE);
    }

    @Override
    public long getCurrentPosition() {
        if (null != mProvider) {
            return mProvider.getCurrentPosition();
        }
        return System.currentTimeMillis() - mStartRenderTs;
    }

    @Override
    public long getPollingInterval() {
        if (null != mProvider) {
            return mProvider.getPollingInterval();
        }
        return 50;
    }
}
