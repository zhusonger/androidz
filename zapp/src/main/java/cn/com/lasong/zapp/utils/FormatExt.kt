package cn.com.lasong.zapp.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/8/15
 * Description:
 * 格式化扩展
 */

/**
 * 转化成时间, 单位为ms
 */
fun Long.formatTime(unit: TimeUnit = TimeUnit.MILLISECONDS): String {
    val ms = unit.toMillis(this)
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return simpleDateFormat.format(ms)
}

/**
 * 转化成时长, 单位为ms
 */
fun Long.formatDuration(unit: TimeUnit = TimeUnit.MILLISECONDS): String {
    val hours = unit.toHours(this)
    val minutes = unit.toMinutes(this) - hours * 60
    val seconds = unit.toSeconds(this) - hours * 3600 - minutes * 60
    return if (hours > 0) {
        String.format(
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds)
    } else {
        String.format(
            "%02d:%02d",
            minutes,
            seconds)
    }
}