package cn.com.lasong.zapp.data

import android.os.Parcel
import android.os.Parcelable

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/19
 * Description:
 */

fun <T:Parcelable> Parcelable.copy(): T? {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(this, 0)
    parcel.setDataPosition(0)
    val copy: T? = parcel.readParcelable(this.javaClass.classLoader)
    parcel.recycle()
    return copy
}