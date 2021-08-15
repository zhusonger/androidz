package cn.com.lasong.zapp.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.BLOB
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "video")
@Parcelize
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var direction: Int,
    var path: String,   // 视频路径
    var uri: String? = null,
    var title: String? = null, // 名称, 默认为系统生成的视频名称
    var duration: Long = 0, // 时长, s
    var favorite: Boolean = false, // 喜好
    var createTime: Long = 0, // 创建的时间戳
    @ColumnInfo(typeAffinity = BLOB)
    var screenshot: ByteArray? = null
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoEntity

        if (id != other.id) return false
        if (path != other.path) return false
        if (uri != other.uri) return false
        if (screenshot != null) {
            if (other.screenshot == null) return false
            if (!screenshot.contentEquals(other.screenshot)) return false
        } else if (other.screenshot != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + path.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (screenshot?.contentHashCode() ?: 0)
        return result
    }
}