package com.example.repository

import com.example.db.TaskDao
import com.example.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTasksForWeek(week: Int): Flow<List<Task>> = taskDao.getTasksForWeek(week)

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteById(id: Long) {
        taskDao.deleteTaskById(id)
    }
}
