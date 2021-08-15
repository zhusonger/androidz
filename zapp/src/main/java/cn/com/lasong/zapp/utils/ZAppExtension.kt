package cn.com.lasong.zapp.utils

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/8/15
 * Description:
 * Glide Extensions
 */
private const val MINI_THUMB_SIZE = 200
fun GlideRequest<*>.miniThumb(size: Int = MINI_THUMB_SIZE): GlideRequest<*> {
    return centerCrop()
        .override(size)
}