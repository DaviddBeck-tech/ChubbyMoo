package com.example.ui

import android.app.AlertDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Task
import com.example.model.Category
import com.example.viewmodel.TaskViewModel
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import org.json.JSONArray
import org.json.JSONObject

data class MonthlyGoal(
    val id: String,
    val title: String,
    val completed: Boolean,
    val yearMonth: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SundayRitualDialog(
    allTasks: List<Task>,
    onDismiss: () -> Unit,
    taskViewModel: TaskViewModel
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("week") } // "week" or "month"
    
    // Monthly goals state and persistence inside SharedPreferences
    val sharedPref = remember { context.getSharedPreferences("sunday_ritual_prefs", Context.MODE_PRIVATE) }

    var monthlyGoals by remember {
        mutableStateOf(
            try {
                val json = sharedPref.getString("goals", null)
                if (json != null) {
                    val arr = JSONArray(json)
                    val list = mutableListOf<MonthlyGoal>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            MonthlyGoal(
                                id = obj.optString("id", ""),
                                title = obj.optString("title", ""),
                                completed = obj.optBoolean("completed", false),
                                yearMonth = obj.optString("yearMonth", "")
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    // Safely save goals on list change
    fun saveGoals(newList: List<MonthlyGoal>) {
        monthlyGoals = newList
        try {
            val arr = JSONArray()
            for (goal in newList) {
                val obj = JSONObject()
                obj.put("id", goal.id)
                obj.put("title", goal.title)
                obj.put("completed", goal.completed)
                obj.put("yearMonth", goal.yearMonth)
                arr.put(obj)
            }
            sharedPref.edit().putString("goals", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val currentYearMonth = remember { LocalDate.now().toString().substring(0, 7) } // Format YYYY-MM

    // Quick task addition text state
    var showQuickAddDialogForDayIndex by remember { mutableStateOf<Int?>(null) }
    var quickTaskTitle by remember { mutableStateOf("") }

    val daysOfWeekInfo = remember {
        listOf(
            Triple(0, "Thứ 2", "T2"),
            Triple(1, "Thứ 3", "T3"),
            Triple(2, "Thứ 4", "T4"),
            Triple(3, "Thứ 5", "T5"),
            Triple(4, "Thứ 6", "T6"),
            Triple(5, "Thứ 7", "T7"),
            Triple(6, "Chủ Nhật", "CN")
        )
    }

    fun getUpcomingDateString(dayIndex: Int): String {
        val today = LocalDate.now()
        // Align to the coming week
        val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val targetDate = nextMonday.plusDays(dayIndex.toLong())
        return targetDate.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, PrimaryLavender, RoundedCornerShape(24.dp)),
            color = BackgroundCream
        ) {
            Scaffold(
                containerColor = BackgroundCream,
                topBar = {
                    Column(
                        modifier = Modifier
                            .background(BackgroundCream)
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Nghi Thức Chủ Nhật ☕",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = BackgroundCream
                            ),
                            actions = {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đóng",
                                        tint = TextCharcoal
                                    )
                                }
                            }
                        )

                        // Pastel switching tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(Color.White, RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { activeTab = "week" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTab == "week") PrimaryLavender else Color.Transparent,
                                    contentColor = TextCharcoal
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "📅 Lịch Tuần Tới",
                                    fontWeight = if (activeTab == "week") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { activeTab = "month" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTab == "month") SecondaryPink else Color.Transparent,
                                    contentColor = TextCharcoal
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "🎯 Mục Tiêu Tháng",
                                    fontWeight = if (activeTab == "month") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (activeTab == "week") {
                        // --- TAB 1: WEEK PLANNING ---
                        Text(
                            text = "Hôm nay là thời khắc dọn dẹp tâm trí! Hãy thong thả lên lịch sớm cho cả tuần học tập & làm việc tuần tới để đón tuần mới cực thảnh thơi nhé! 💕",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )

                        daysOfWeekInfo.forEach { (index, dayLabel, dayShort) ->
                            val dateStr = getUpcomingDateString(index)
                            val tasksForDay = allTasks.filter { it.scheduledDate == dateStr }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, shape = RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryLavender),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayShort,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextCharcoal
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = dayLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextCharcoal
                                                )
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextGray
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { 
                                                showQuickAddDialogForDayIndex = index
                                                quickTaskTitle = ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SecondaryPink.copy(alpha = 0.5f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = "+ Thêm nhanh",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextCharcoal,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (tasksForDay.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            tasksForDay.forEach { task ->
                                                val isDone = task.status == Task.STATUS_COMPLETED
                                                val cat = Category.getById(task.categoryId)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(cat.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(text = cat.iconEmoji, fontSize = 12.sp)
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                        color = if (isDone) TextCharcoal.copy(alpha = 0.5f) else TextCharcoal,
                                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                    Text(
                                                        text = task.reminderTime,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextGray,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Không có công việc nào trong ngày này.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 40.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- TAB 2: MONTHLY GOALS ---
                        Text(
                            text = "Lên danh sách các mục tiêu lớn, cam kết mốc quan trọng tiếp theo của tháng này rồi tích mốc xong rực rỡ cùng Bò Béo nhé! 🎯🐮",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )

                        // Input container for custom goal
                        var newGoalText by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newGoalText,
                                onValueChange = { newGoalText = it },
                                placeholder = { Text("Ví dụ: Đọc xong 2 cuốn sách 📖", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocusPink,
                                    unfocusedBorderColor = SecondaryPink
                                )
                            )

                            Button(
                                onClick = {
                                    if (newGoalText.trim().isNotEmpty()) {
                                        val newGoalObj = MonthlyGoal(
                                            id = "goal_${System.currentTimeMillis()}",
                                            title = newGoalText.trim(),
                                            completed = false,
                                            yearMonth = currentYearMonth
                                        )
                                        saveGoals(monthlyGoals + newGoalObj)
                                        newGoalText = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Thêm",
                                    tint = TextCharcoal
                                )
                            }
                        }

                        // Displays list of active goals
                        val activeGoals = monthlyGoals.filter { it.yearMonth == currentYearMonth }
                        if (activeGoals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có mục tiêu hành động nào được tạo trong tháng!\nCậu hãy viết mục tiêu đầu tiên lên nhé! 🌱",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                activeGoals.forEach { goal ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                // Checkbox frame click
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (goal.completed) SuccessMintBorder else Color.Transparent)
                                                        .border(2.dp, SuccessMintBorder, CircleShape)
                                                        .clickable {
                                                            val updatedList = monthlyGoals.map {
                                                                if (it.id == goal.id) it.copy(completed = !goal.completed) else it
                                                            }
                                                            saveGoals(updatedList)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (goal.completed) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = goal.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (goal.completed) TextCharcoal.copy(alpha = 0.5f) else TextCharcoal,
                                                    textDecoration = if (goal.completed) TextDecoration.LineThrough else TextDecoration.None,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val filtered = monthlyGoals.filter { it.id != goal.id }
                                                    saveGoals(filtered)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Xóa",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Floating text prompt dialogue for adding next week's task
    if (showQuickAddDialogForDayIndex != null) {
        val dayIdx = showQuickAddDialogForDayIndex!!
        val targetDayLabel = daysOfWeekInfo[dayIdx].second
        val targetDateStr = getUpcomingDateString(dayIdx)

        AlertDialog(
            onDismissRequest = { showQuickAddDialogForDayIndex = null },
            title = {
                Text(
                    text = "Lên Lịch Nhanh 🌟",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tạo công việc mới cho ngày $targetDayLabel ($targetDateStr):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextCharcoal
                    )

                    OutlinedTextField(
                        value = quickTaskTitle,
                        onValueChange = { quickTaskTitle = it },
                        placeholder = { Text("Ví dụ: Đi bộ 15 phút 🌱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_task_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickTaskTitle.trim().isNotEmpty()) {
                            taskViewModel.addTask(
                                title = quickTaskTitle.trim(),
                                description = "Được lên lịch sớm trong nghi thức chuẩn bị Chủ Nhật.",
                                categoryId = 3, // Custom/Personal category ID
                                date = targetDateStr,
                                time = "09:00"
                            )
                            showQuickAddDialogForDayIndex = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender)
                ) {
                    Text("Tạo Lịch", color = TextCharcoal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialogForDayIndex = null }) {
                    Text("Hủy", color = TextCharcoal)
                }
            }
        )
    }
}
