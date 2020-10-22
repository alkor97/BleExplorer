package info.alkor.bleinquirer.persistence

import androidx.room.*

@Dao
interface NameMappingDao {
    @Query("SELECT * from name_mapping")
    fun all(): List<NameMapping>

    @Query("SELECT name FROM name_mapping WHERE address = :address")
    fun getNameOf(address: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun storeNameMapping(record: NameMapping)
}

@Entity(tableName = "name_mapping")
data class NameMapping(
    @PrimaryKey val address: String,
    val name: String
)