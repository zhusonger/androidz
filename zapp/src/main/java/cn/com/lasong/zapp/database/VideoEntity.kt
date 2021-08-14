package cn.com.lasong.zapp.database

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.BLOB
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class VideoEntity(@PrimaryKey var path: String,
                       var uri: String? = null,
                       @ColumnInfo(typeAffinity = BLOB)
                       var screenshot: ByteArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoEntity

        if (path != other.path) return false
        if (uri != other.uri) return false
        if (screenshot != null) {
            if (other.screenshot == null) return false
            if (!screenshot.contentEquals(other.screenshot)) return false
        } else if (other.screenshot != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (screenshot?.contentHashCode() ?: 0)
        return result
    }
}