package cn.com.lasong.zapp.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class VideoEntity(@PrimaryKey var path: String, var uri: String?);