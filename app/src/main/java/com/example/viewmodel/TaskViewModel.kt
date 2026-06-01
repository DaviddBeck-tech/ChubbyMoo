package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.Task
import com.example.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
        // Seed initial cute data if DB is empty
        viewModelScope.launch {
            repository.allTasks.first().let { currentTasks ->
                if (currentTasks.isEmpty()) {
                    seedDummyTasks()
                }
            }
        }
    }

    // Selected Date to show tasks (Format: YYYY-MM-DD), defaults to today
    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // All Tasks state
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered tasks for the selected date
    val tasksForSelectedDate: StateFlow<List<Task>> = combine(allTasks, _selectedDate) { tasks, date ->
        tasks.filter { it.scheduledDate == date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected week of year for the week statistics
    val currentWeekOfYear: StateFlow<Int> = _selectedDate.map { dateStr ->
        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            LocalDate.now()
        }
        date.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR)
    )

    // Week stats: Count of completed and total tasks for current week
    val weekProgress: StateFlow<WeekProgress> = combine(allTasks, currentWeekOfYear) { tasks, week ->
        val weeklyTasks = tasks.filter { it.creationWeek == week }
        val completed = weeklyTasks.count { it.status == Task.STATUS_COMPLETED }
        val total = weeklyTasks.size
        WeekProgress(completed, total)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeekProgress(0, 0)
    )

    fun changeSelectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    // Operations
    fun addTask(title: String, description: String, categoryId: Int, date: String, time: String) {
        viewModelScope.launch {
            val parsedDate = try {
                LocalDate.parse(date)
            } catch (e: Exception) {
                LocalDate.now()
            }
            val dateStr = parsedDate.toString()
            val weekOfYear = parsedDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
            val newTask = Task(
                title = title,
                description = description,
                categoryId = categoryId,
                status = Task.STATUS_PENDING,
                creationWeek = weekOfYear,
                scheduledDate = dateStr,
                reminderTime = time,
                originalDate = dateStr
            )
            repository.insert(newTask)
        }
    }

    fun toggleTaskComplete(task: Task, proofImage: String? = null, completedAt: String? = null) {
        viewModelScope.launch {
            val isCompleted = task.status == Task.STATUS_COMPLETED
            val newStatus = if (isCompleted) {
                if (task.originalDate != task.scheduledDate) Task.STATUS_MOVED else Task.STATUS_PENDING
            } else {
                Task.STATUS_COMPLETED
            }
            val updatedTask = task.copy(
                status = newStatus,
                proofImage = if (isCompleted) null else (proofImage ?: task.proofImage),
                completedAt = if (isCompleted) null else (completedAt ?: task.completedAt)
            )
            repository.update(updatedTask)
        }
    }

    // Crucial Rescheduling / Snoozing feature (Snooze to Tomorrow: adds 1 day, Snooze to Next Week: adds 7 days)
    fun postponeTask(task: Task, daysToAdd: Long) {
        viewModelScope.launch {
            val currentDate = LocalDate.parse(task.scheduledDate)
            val newDate = currentDate.plusDays(daysToAdd)
            val newWeek = newDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
            
            val updatedTask = task.copy(
                status = Task.STATUS_MOVED,
                scheduledDate = newDate.toString(),
                creationWeek = newWeek,
                originalDate = task.originalDate ?: task.scheduledDate // Keep tracker of original scheduling
            )
            repository.update(updatedTask)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteById(taskId)
        }
    }

    private suspend fun seedDummyTasks() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val indexSunday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val dummyTasks = listOf(
            Task(
                title = "☕ Trà chiều cùng bạn bè",
                description = "Gặp gỡ bạn bè tại tiệm trà Cozy Pastel, tán gẫu cuối tuần cực vui.",
                categoryId = 3, // Personal (Pastel Yellow)
                status = Task.STATUS_PENDING,
                creationWeek = indexSunday.get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                scheduledDate = indexSunday.toString(),
                reminderTime = "15:30",
                originalDate = indexSunday.toString()
            ),
            Task(
                title = "🌱 Luyện tập yoga thư giãn",
                description = "Giải tỏa căng thẳng với 30 phút tập yoga nhẹ nhàng và hít thở sâu.",
                categoryId = 4, // Health (Mint Green)
                status = Task.STATUS_PENDING,
                creationWeek = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                scheduledDate = today.toString(),
                reminderTime = "07:00",
                originalDate = today.toString()
            ),
            Task(
                title = "💼 Chuẩn bị tài liệu thuyết trình",
                description = "Hoàn thành slide báo cáo và nghiên cứu các chỉ số kế hoạch tuần mới.",
                categoryId = 1, // Work (Pastel Purple)
                status = Task.STATUS_PENDING,
                creationWeek = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                scheduledDate = today.toString(),
                reminderTime = "09:30",
                originalDate = today.toString()
            ),
            Task(
                title = "📝 Đọc sách Quicksand",
                description = "Đọc chương 4 và 5 cuốn sách định dạng phong cách sống tối giản.",
                categoryId = 2, // Study (Soft Pink)
                status = Task.STATUS_COMPLETED,
                creationWeek = tomorrow.get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                scheduledDate = tomorrow.toString(),
                reminderTime = "21:00",
                originalDate = tomorrow.toString()
            )
        )

        for (task in dummyTasks) {
            repository.insert(task)
        }
    }

    data class WeekProgress(val completed: Int, val total: Int) {
        val percentage: Float
            get() = if (total == 0) 0f else (completed.toFloat() / total)
    }

    // Simple ViewModel factory
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
