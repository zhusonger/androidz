package cn.com.lasong.zapp.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cn.com.lasong.zapp.ZApp.Companion.applicationContext

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/8/11
 * Description:
 * ZApp数据库
 */
@Database(version = 1, exportSchema = false, entities = [VideoEntity::class])
abstract class ZAppDatabase : RoomDatabase() {
    // VideoDao is a class annotated with @Dao.
    abstract fun getVideoDao(): VideoDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: ZAppDatabase? = null

        fun getDatabase(): ZAppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    applicationContext(),
                    ZAppDatabase::class.java,
                    "zapp_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}