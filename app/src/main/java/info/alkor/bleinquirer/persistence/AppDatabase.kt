package info.alkor.bleinquirer.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NameMapping::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nameMappingDao(): NameMappingDao

    companion object {
        @Volatile
        private var db: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var current = db
                if (current == null) {
                    current =
                        Room.databaseBuilder(context, AppDatabase::class.java, "BleInquirerDb")
                            .fallbackToDestructiveMigration()
                            .build()
                    db = current
                }
                return current
            }
        }
    }
}