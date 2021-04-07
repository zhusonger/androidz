package cn.com.lasong.widget.text;


import java.io.Serializable;

public class RichText implements Serializable, Cloneable {
    private static final long serialVersionUID = -601848252661601162L;

    // 普通文字
    public static final int TYPE_NORMAL = -1;
    // 富文本
    public static final int TYPE_RICH_TEXT = 1;
    // image
    public static final int TYPE_IMAGE = 2;
    // link
    public static final int TYPE_LINK = 3;
    // 本地图片
    public static final int TYPE_LOCAL_DRAWABLE = 5;

    // 001
    public static final int FONT_STYLE_BOLD = 1;
    // 010
    public static final int FONT_STYLE_ITALIC = 2;
    // 100
    public static final int FONT_STYLE_UNDERLINE = 4;

    // 字体颜色
    public String fontColor;
    // 字体大小等级，非具体字体大小数值
    public int fontSizeLevel;
    // 字体背景色
    public String backgroundColor;
    // 字体样式，粗体斜体等
    /**
     * {@link #FONT_STYLE_BOLD#FONT_STYLE_ITALIC#FONT_STYLE_UNDERLINE }
     */
    public int style;
    /**
     * 类型 {@link #TYPE_NORMAL}
     */
    public int type = TYPE_NORMAL;
    /**
     * {@link #TYPE_RICH_TEXT}
     */
    public String content;
    /**
     * {@link #TYPE_IMAGE}
     */
    public String image;
    /**
     * {@link #TYPE_LINK}
     */
    public String url;
    public String name;

}