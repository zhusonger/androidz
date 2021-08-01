package cn.com.lasong.zapp.data

import android.opengl.Matrix
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordVideo(
    val index: Int,
    var _width: Int = 0,
    var _height: Int = 0,
    var bitrate: Int = 0,
    var dpi: Int = 1,
    var matrix: FloatArray = FloatArray(16) // 投影矩阵
) : Parcelable {
    // 根据方向来矫正宽高
    fun coerceDirection(direction: Int) {
        val width = _width
        val height = _height
        // 纠正屏幕的宽高
        if (direction == DIRECTION_LANDSCAPE) {
            _width = width.coerceAtLeast(height)
            _height = height.coerceAtMost(width)
        } else {
            _height = width.coerceAtLeast(height)
            _width = height.coerceAtMost(width)
        }
    }

    // 调整宽高跟手机分辨率比例靠近
    fun alignToMobileRatio() {
        if (_width >= _height) {
            _width = (_height * MOBILE_RATIO + 0.5f).toInt()
        } else {
            _height = (_width * MOBILE_RATIO + 0.5f).toInt()
        }
    }

    // 获取真实画面与实际显示画面的投影矩阵
    fun updateMatrix(clipMode: Int, swapWH: Boolean = false) {
        val actualRatio = if (swapWH) {
            actualHeight.toFloat() / actualWidth
        } else {
            actualWidth.toFloat() / actualHeight
        }
        val renderRatio = if (swapWH) {
            renderHeight.toFloat() / renderWidth
        } else {
            renderWidth.toFloat() / renderHeight
        }
        // 举例
        // 横屏
        // actualRatio 1: 1280/960=16/12=1.333
        // actualRatio 2: 1280/640=16/8=2
        // renderRatio 1280/720=16/9=1.778
        // 竖屏
        // actualRatio 1: 960/1280=12/16=0.75
        // actualRatio 2: 640/1280=8/16=0.5
        // renderRatio 720/1280=9/16=0.5625

        var left = -1f
        var right = 1f
        var bottom = -1f
        var top = 1f
        if (clipMode == CLIP_FILL) {
            // FILL 居中完整显示, 其余空间填充
            // 所以这里要调整视口变大, 容纳完整画面
            // 1. AR <= RR 实际比渲染小
            // 说明[宽]相同的情况下, 高度:渲染<实际,
            // 所以在FILL模式, 实际的高度缩小到渲染高度, 保持比例的情况下
            // 实际宽度也缩小到渲染范围内, 以高度为基准, 宽度调整, 如下
            //  ------------         --------
            // |  |||||||   |       |  ||||| |
            // |  |||||||   |       |  ||||| |
            //  ------------        |  ||||| |
            //                      |  ||||| |
            //                       ---------
            if (actualRatio <= renderRatio) {
                // top&bottom不变, 调整left&right
                left = -(renderRatio / actualRatio)
                right = renderRatio / actualRatio
            }
            // 2. AR > RR
            // 说明[高]相同的情况下, 宽度:实际>渲染
            // 在FILL模式, 实际的宽度缩小大渲染宽度, 保持比例的情况下
            // 实际的宽度缩小到渲染范围内, 以宽度为基准, 高度调整, 如下
            //  ------------         --------
            // |            |       |        |
            // ||||||||||||||       ||||||||||
            // ||||||||||||||       ||||||||||
            // |            |       ||||||||||
            //  ------------        |        |
            //                       --------
            else {
                // left&right不变, 调整top&bottom
                bottom = -(actualRatio / renderRatio)
                top = actualRatio / renderRatio
            }
        } else if (clipMode == CLIP_CENTER) {
            // CENTER 居中裁剪显示, 小边撑满
            // 所以这里要调整视口变小, 进行裁剪
            if (actualRatio <= renderRatio) {
                // top&bottom不变, 调整left&right
                left = -(actualRatio / renderRatio)
                right = actualRatio / renderRatio
            }
            // 2. AR > RR
            else {
                // left&right不变, 调整top&bottom
                bottom = -(renderRatio / actualRatio)
                top = renderRatio / actualRatio
            }
        }
        Matrix.orthoM(matrix, 0, left, right, bottom, top, 1f, 3f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordVideo

        if (index != other.index) return false
        if (_width != other._width) return false
        if (_height != other._height) return false
        if (bitrate != other.bitrate) return false
        if (dpi != other.dpi) return false
        if (!matrix.contentEquals(other.matrix)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + _width
        result = 31 * result + _height
        result = 31 * result + bitrate
        result = 31 * result + dpi
        result = 31 * result + matrix.contentHashCode()
        return result
    }

    // 做16位对齐, MediaCodec硬解码不是16位对齐会花屏, 安全起见, 输出都16位对齐
    @IgnoredOnParcel
    val renderWidth: Int
        get() {
            val offset = _width % 16
            if (offset != 0) {
                return _width + (16 - offset)
            }
            return _width
        }

    @IgnoredOnParcel
    val renderHeight: Int
        get() {
            val offset = _height % 16
            if (offset != 0) {
                return _height + (16 - offset)
            }
            return _height
        }

    // 实际的内容宽高
    @IgnoredOnParcel
    val actualWidth: Int
        get() = _width

    @IgnoredOnParcel
    val actualHeight: Int
        get() = _height
}