package cn.com.lasong.widget.shadow.v2;

/**
 * 阴影模式, 默认 WRAP_ALL
 */
public enum ShadowMode {
    WRAP_ALL(0),
    FILL_ALL(1),
    FILL_W_WARP_H(2),
    WARP_W_FILL_H(3);

    private final int value;
    ShadowMode(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}