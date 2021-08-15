package cn.com.lasong.zapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideo(vararg video: VideoEntity)

    @Query("SELECT screenshot FROM video ORDER BY id DESC LIMIT 1")
    fun queryLastThumbnail(): ByteArray?
}