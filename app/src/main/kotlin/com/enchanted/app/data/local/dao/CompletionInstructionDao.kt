package com.enchanted.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enchanted.app.data.local.entity.CompletionInstructionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionInstructionDao {

    @Query("SELECT * FROM completion_instructions ORDER BY `order` ASC")
    fun getAllInstructions(): Flow<List<CompletionInstructionEntity>>

    @Query("SELECT * FROM completion_instructions ORDER BY `order` ASC")
    suspend fun getAllInstructionsList(): List<CompletionInstructionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(instructions: List<CompletionInstructionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instruction: CompletionInstructionEntity)

    @Update
    suspend fun update(instruction: CompletionInstructionEntity)

    @Delete
    suspend fun delete(instruction: CompletionInstructionEntity)

    @Query("DELETE FROM completion_instructions")
    suspend fun deleteAll()
}
