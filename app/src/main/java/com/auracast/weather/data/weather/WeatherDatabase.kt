package com.auracast.weather.data.weather

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationId: String,
    val snapshotJson: String,
    val timestampMillis: Long,
)

@Dao
interface WeatherCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("SELECT * FROM weather_cache WHERE locationId = :id LIMIT 1")
    suspend fun get(id: String): WeatherCacheEntity?

    @Query("DELETE FROM Weather_cache WHERE timestampMillis < :cutoff")
    suspend fun prune(cutoff: Long)
}

@androidx.room.Database(entities = [WeatherCacheEntity::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : androidx.room.RoomDatabase() {
    abstract fun cacheDao(): WeatherCacheDao
}

// Mapping helpers
fun WeatherSnapshot.toEntity(locationId: String, json: Json): WeatherCacheEntity =
    WeatherCacheEntity(locationId, json.encodeToString(this), System.currentTimeMillis())

fun WeatherCacheEntity.toDomain(json: Json): WeatherSnapshot =
    json.decodeFromString(snapshotJson)
