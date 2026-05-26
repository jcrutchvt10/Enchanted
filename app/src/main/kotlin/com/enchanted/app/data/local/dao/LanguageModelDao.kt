package com.enchanted.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enchanted.app.data.local.entity.LanguageModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageModelDao {

    @Query("SELECT * FROM language_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<LanguageModelEntity>>

    @Query("SELECT * FROM language_models ORDER BY name ASC")
    suspend fun getAllModelsList(): List<LanguageModelEntity>

    @Query("SELECT * FROM language_models WHERE name = :name")
    suspend fun getModel(name: String): LanguageModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<LanguageModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: LanguageModelEntity)

    @Delete
    suspend fun delete(model: LanguageModelEntity)

    @Query("DELETE FROM language_models")
    suspend fun deleteAll()
}
