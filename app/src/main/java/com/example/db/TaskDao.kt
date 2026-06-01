package com.example.db

import androidx.room.*
import com.example.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledDate ASC, reminderTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY reminderTime ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE creationWeek = :week ORDER BY scheduledDate ASC, reminderTime ASC")
    fun getTasksForWeek(week: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Delete
    suspend fun deleteTask(task: Task)
}
