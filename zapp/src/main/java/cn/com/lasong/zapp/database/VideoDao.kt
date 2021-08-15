package cn.com.lasong.zapp.database

import androidx.room.*

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideo(vararg video: VideoEntity)

    @Query("SELECT screenshot FROM video ORDER BY id DESC LIMIT 1")
    fun queryLastThumbnail(): ByteArray?

    @Query("SELECT * FROM video WHERE id < :fromIndex ORDER BY id DESC LIMIT :pageSize")
    fun queryVideos(fromIndex: Int, pageSize: Int): List<VideoEntity>

    @Update
    fun updateVideo(video: VideoEntity)

    @Delete
    fun deleteVideo(video: VideoEntity)
}