package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Category
import com.example.model.Task
import com.example.model.MascotMessages
import com.example.MascotNotificationHelper
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TaskViewModel
import com.example.ui.BrainDumpTriggerCard
import com.example.ui.BrainDumpDialog
import com.example.ui.WeeklyRecapDialog
import com.example.ui.SundayRitualDialog
import com.example.utils.PdfExportHelper
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic notification permission check for Android 13 (Tiramisu) or above
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(permission), 101)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(taskViewModel = taskViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(taskViewModel: TaskViewModel) {
    val selectedDate by taskViewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by taskViewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val progress by taskViewModel.weekProgress.collectAsStateWithLifecycle()
    val allTasks by taskViewModel.allTasks.collectAsStateWithLifecycle()
    val currentWeek by taskViewModel.currentWeekOfYear.collectAsStateWithLifecycle()
    val weeklyTasks = remember(allTasks, currentWeek) { allTasks.filter { it.creationWeek == currentWeek } }
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showBrainDumpDialog by remember { mutableStateOf(false) }
    var showWeeklyRecap by remember { mutableStateOf(false) }
    var showSundayRitual by remember { mutableStateOf(false) }
    var activeTaskForCamera by remember { mutableStateOf<Task?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Edge-to-edge content handling
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lovely Scheduler 🌸",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.TextCharcoal
                        )
                        Text(
                            text = "Lên lịch xinh, làm việc xịn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    // Action button to trigger the smart Weekly Recap dialogue
                    IconButton(
                        onClick = { showWeeklyRecap = true },
                        modifier = Modifier.testTag("weekly_recap_button")
                    ) {
                        Text("📈", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Action button to trigger the Cozy Sunday preparation ritual
                    IconButton(
                        onClick = { showSundayRitual = true },
                        modifier = Modifier.testTag("sunday_ritual_button")
                    ) {
                        Text("☕", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Small action to quickly reset selection to today
                    IconButton(
                        onClick = { taskViewModel.changeSelectedDate(LocalDate.now().toString()) },
                        modifier = Modifier.testTag("reset_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Reset về hôm nay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Cute profile avatar frame mimicking design HTML
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.example.ui.theme.SecondaryPink)
                            .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                            .shadow(1.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.FocusPink)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // --- Brand New Smart Brain-Dump (Brain/Sparkles AI) FAB ---
                val isExpanded by remember {
                    derivedStateOf { scrollState.value <= 100 }
                } // Shrinks to icon-only when scrolled down

                // Floating bobbing animation
                val infiniteTransition = rememberInfiniteTransition(label = "floating_ai_fab")
                val dy by infiniteTransition.animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dy"
                )

                // Animated Width for shrinking
                val widthTransition = animateDpAsState(
                    targetValue = if (isExpanded) 140.dp else 56.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
                    label = "width"
                )

                Box(
                    modifier = Modifier
                        .offset(y = dy.dp)
                        .width(widthTransition.value)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFCCF9), // Soft Pastel Pink
                                    Color(0xFFE0BBE4)  // Soft Lavender
                                )
                            )
                        )
                        .clickable { showBrainDumpDialog = true }
                        .border(3.dp, Color.White, RoundedCornerShape(28.dp))
                        .shadow(4.dp, RoundedCornerShape(28.dp))
                        .testTag("ai_brain_dump_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🧠", fontSize = 24.sp)
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xả não AI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = com.example.ui.theme.TextCharcoal
                            )
                        }
                    }

                    // Floating Mini Badge "AI ✨" in Top-Right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = (2).dp)
                            .background(com.example.ui.theme.FocusPink, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI ✨",
                            fontSize = 7.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- Standard Add Task FAB ---
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary, // #E0BBE4
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .border(3.dp, Color.White, RoundedCornerShape(20.dp))
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .testTag("add_task_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm công việc",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Week View Calendar Component
            WeekDaySelector(
                selectedDateStr = selectedDate,
                onDateSelected = { taskViewModel.changeSelectedDate(it) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Weekly summary Card with Progress
            WeeklyProgressCard(progress = progress)

            Spacer(modifier = Modifier.height(10.dp))

            // Integration of the Duolingo Mascot Status component
            MascotStatusCard(
                tasks = tasks,
                onTriggerNotification = { status ->
                    MascotNotificationHelper.triggerNotification(context, status)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Task List Title
            val parsedDate = try {
                LocalDate.parse(selectedDate)
            } catch (e: Exception) {
                LocalDate.now()
            }
            val todayStr = LocalDate.now().toString()
            val tomorrowStr = LocalDate.now().plusDays(1).toString()
            val listTitle = when (selectedDate) {
                todayStr -> "Hôm nay"
                tomorrowStr -> "Ngày mai"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.forLanguageTag("vi"))
                    parsedDate.format(formatter)
                }
            }

            Text(
                text = "$listTitle 📝",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic list of tasks for the day
            if (tasks.isEmpty()) {
                EmptyTasksState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tasks.forEach { task ->
                        key(task.id) {
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { 
                                    val wasCompleted = task.status == Task.STATUS_COMPLETED
                                    if (!wasCompleted) {
                                        activeTaskForCamera = task
                                        showCameraDialog = true
                                    } else {
                                        taskViewModel.toggleTaskComplete(task)
                                    }
                                },
                                onPostpone = { days -> 
                                    taskViewModel.postponeTask(task, days)
                                    MascotNotificationHelper.triggerNotification(context, MascotMessages.STATE_SAD_PONTED)
                                },
                                onDelete = { taskViewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }

        // Add Task Dialog
        if (showAddDialog) {
            AddTaskDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, catId, date, time ->
                    taskViewModel.addTask(title, desc, catId, date, time)
                    showAddDialog = false
                }
            )
        }

        // Brain Dump AI Assistant Dialog
        if (showBrainDumpDialog) {
            BrainDumpDialog(
                onDismiss = { showBrainDumpDialog = false },
                onSaveTasks = { parsedTasks ->
                    for (item in parsedTasks) {
                        taskViewModel.addTask(
                            title = item.title,
                            description = item.reason,
                            categoryId = 1, // Default (Work / Soft Lavender)
                            date = item.suggestedDate,
                            time = "09:00"
                        )
                    }
                    showBrainDumpDialog = false
                }
            )
        }

        // Custom Camera Dialog
        val taskToCapture = activeTaskForCamera
        if (showCameraDialog && taskToCapture != null) {
            com.example.ui.CustomCameraDialog(
                taskTitle = taskToCapture.title,
                onDismiss = {
                    showCameraDialog = false
                    activeTaskForCamera = null
                },
                onCapture = { imageUri, timeText ->
                    taskViewModel.toggleTaskComplete(taskToCapture, imageUri, timeText)
                    com.example.MascotNotificationHelper.triggerNotification(context, com.example.model.MascotMessages.STATE_HAPPY)
                    showCameraDialog = false
                    activeTaskForCamera = null
                }
            )
        }

        // Weekly Recap Dialog with smart PDF export capability
        if (showWeeklyRecap) {
            WeeklyRecapDialog(
                tasks = weeklyTasks,
                onDismiss = { showWeeklyRecap = false },
                onExportPdf = { selectedTasks ->
                    PdfExportHelper.exportWeeklyReport(context, selectedTasks)
                }
            )
        }

        // Cozy Sunday Ritual planner & monthly goals commitment Flow
        if (showSundayRitual) {
            SundayRitualDialog(
                allTasks = allTasks,
                onDismiss = { showSundayRitual = false },
                taskViewModel = taskViewModel
            )
        }
    }
}

@Composable
fun WeekDaySelector(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit
) {
    val selectedDate = try {
        LocalDate.parse(selectedDateStr)
    } catch (e: Exception) {
        LocalDate.now()
    }
    // Always calculate Monday of the week corresponding to selectedDate
    val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("vi"))
        Text(
            text = "Tháng " + selectedDate.format(monthFormatter).replaceFirstChar { it.lowercase() },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEachIndexed { index, day ->
                val isSelected = day.toString() == selectedDateStr
                val isToday = day.toString() == LocalDate.now().toString()
                
                // Day label translation
                val dayLabel = when (day.dayOfWeek) {
                    DayOfWeek.MONDAY -> "T2"
                    DayOfWeek.TUESDAY -> "T3"
                    DayOfWeek.WEDNESDAY -> "T4"
                    DayOfWeek.THURSDAY -> "T5"
                    DayOfWeek.FRIDAY -> "T6"
                    DayOfWeek.SATURDAY -> "T7"
                    DayOfWeek.SUNDAY -> "CN"
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                isToday -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(day.toString()) }
                        .padding(vertical = 10.dp)
                        .testTag("day_capsule_$index")
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressCard(progress: TaskViewModel.WeekProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, shape = MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tiến độ tuần này 🌸",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (progress.total == 0) {
                            "Hãy tạo công việc đầu tiên cho tuần nhé!"
                        } else {
                            "Đã hoàn thành ${progress.completed}/${progress.total} công việc."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Text(
                        text = "${(progress.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cute pastel progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = if (progress.percentage == 0f) 0.05f else progress.percentage)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onPostpone: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val category = Category.getById(task.categoryId)
    val isCompleted = task.status == Task.STATUS_COMPLETED
    val isMoved = task.status == Task.STATUS_MOVED

    // Precise color mappings from the design layout specs
    val containerColor = when {
        isCompleted -> com.example.ui.theme.SuccessMint
        isMoved -> com.example.ui.theme.WarningPastel
        else -> category.color
    }

    val borderColor = when {
        isCompleted -> com.example.ui.theme.SuccessMintBorder
        isMoved -> com.example.ui.theme.WarningPastelBorder
        else -> category.color.copy(alpha = 0.8f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape = RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("task_item_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Task Information details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.iconEmoji,
                            modifier = Modifier.padding(end = 4.dp),
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCompleted || isMoved) category.color 
                                    else Color.White.copy(alpha = 0.6f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.nameVi,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.TextCharcoal
                            )
                        }

                        if (isMoved && task.originalDate != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Đã dời lịch",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = com.example.ui.theme.WarningPastelText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) com.example.ui.theme.TextCharcoal.copy(alpha = 0.5f) else com.example.ui.theme.TextCharcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isCompleted) com.example.ui.theme.TextCharcoal.copy(alpha = 0.4f) else com.example.ui.theme.TextCharcoal.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Time display
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = com.example.ui.theme.TextCharcoal.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMoved && task.originalDate != null) "Dời từ ${task.originalDate} • ${task.reminderTime}" else task.reminderTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.TextCharcoal.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Custom badge status directly matching mockup layout (Xong / Chờ / Đã dời)
                val statusText = when {
                    isCompleted -> "Xong"
                    isMoved -> "Dời"
                    else -> "Chờ"
                }
                val statusBgColor = when {
                    isCompleted -> Color.White.copy(alpha = 0.4f)
                    isMoved -> com.example.ui.theme.WarningPastelBorder
                    else -> com.example.ui.theme.FocusPink
                }
                val statusTextColor = when {
                    isMoved -> com.example.ui.theme.WarningPastelText
                    isCompleted -> com.example.ui.theme.TextCharcoal.copy(alpha = 0.7f)
                    else -> Color.White
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }

                    // Checkbox toggle control
                    if (!(isCompleted && task.proofImage != null)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    width = if (isCompleted) 0.dp else 2.dp,
                                    color = if (isCompleted) Color.Transparent else com.example.ui.theme.FocusPink,
                                    shape = CircleShape
                                )
                                .clickable { onToggleComplete() }
                                .padding(6.dp)
                                .testTag("task_checkbox_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(com.example.ui.theme.SuccessMintBorder)
                                )
                            }
                        }
                    }
                }
            }

            if (isCompleted && task.proofImage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    val context = LocalContext.current
                    coil.compose.AsyncImage(
                        model = task.proofImage,
                        contentDescription = "Ảnh check-in hoàn thành",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (task.completedAt != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✨ Done lúc ${task.completedAt}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, com.example.ui.theme.SecondaryPink, CircleShape)
                            .clickable {
                                com.example.utils.ImageDownloadHelper.saveImageToGallery(context, task.proofImage)
                            }
                            .testTag("download_proof_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📥", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action section: Postpone & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Postpone / Reschedule buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCompleted) {
                        Surface(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onPostpone(1) } // Snooze +1 Day
                                .testTag("postpone_tomorrow_${task.id}"),
                            color = Color.White.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "+1 Ngày ➡️",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = com.example.ui.theme.TextCharcoal
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onPostpone(7) } // Snooze +7 Days (Next Week)
                                .testTag("postpone_next_week_${task.id}"),
                            color = Color.White.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "+7 Ngày 🗓️",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = com.example.ui.theme.TextCharcoal
                            )
                        }
                    } else {
                        Button(
                            onClick = onToggleComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.6f),
                                contentColor = com.example.ui.theme.TextCharcoal
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("redo_task_button")
                        ) {
                            Text(
                                text = "↺ Làm lại",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.TextCharcoal
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("task_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Xóa công việc",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTasksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🍵🍰",
                fontSize = 42.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Thật thảnh thơi nhàn nhã!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Không có công việc nào cần làm hôm nay cả. Hãy nghỉ ngơi, pha một ly trà ấm rồi thư giãn nhé!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, categoryId: Int, date: String, time: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableIntStateOf(1) }
    var dateStr by remember { mutableStateOf(defaultDate) }
    var timeStr by remember { mutableStateOf("09:00") }

    val context = LocalContext.current
    val dialogContext = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                break
            }
            val base = currentContext.baseContext
            if (base === currentContext || base == null) {
                break
            }
            currentContext = base
        }
        currentContext
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lên lịch mới ✨",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Task Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên công việc") },
                    placeholder = { Text("Ví dụ: Mua dâu tây tươi 🍓") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                )

                // Task description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Ghi chú bổ sung") },
                    placeholder = { Text("Mua dâu chín mọng ngọt lịm...") },
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                )

                // Category selection Label
                Text(
                    text = "Chọn phân loại 🏷️",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                // Category horizontal list of chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Category.Categories.forEach { cat ->
                        val isSelected = cat.id == categoryId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (isSelected) cat.color
                                    else cat.color.copy(alpha = 0.25f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { categoryId = cat.id }
                                .padding(vertical = 8.dp)
                                .testTag("category_chip_${cat.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = cat.iconEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cat.nameVi,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date & Time pickers triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date selection Card trigger
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val currentObj = try {
                                    LocalDate.parse(dateStr)
                                } catch (e: Exception) {
                                    LocalDate.now()
                                }
                                val activity = dialogContext as? android.app.Activity
                                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                                    try {
                                        DatePickerDialog(
                                            activity,
                                            { _, year, month, dayOfMonth ->
                                                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                                dateStr = selectedDate.toString()
                                            },
                                            currentObj.year,
                                            currentObj.monthValue - 1,
                                            currentObj.dayOfMonth
                                        ).show()
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    android.util.Log.e("AddTaskDialog", "No active Activity context available to show DatePickerDialog")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Time selection Card trigger
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val parts = timeStr.split(":")
                                val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val activity = dialogContext as? android.app.Activity
                                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                                    try {
                                        TimePickerDialog(
                                            activity,
                                            { _, hour, minute ->
                                                timeStr = String.format("%02d:%02d", hour, minute)
                                            },
                                            initialHour,
                                            initialMinute,
                                            true
                                        ).show()
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    android.util.Log.e("AddTaskDialog", "No active Activity context available to show TimePickerDialog")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle, // using a nice alternative placeholder clock icon
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        onSave(title, description, categoryId, dateStr, timeStr)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Lên lịch", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_task_button")
            ) {
                Text("Hủy", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}



@Composable
fun MascotStatusCard(
    tasks: List<Task>,
    onTriggerNotification: (String) -> Unit
) {
    val completedTasks = tasks.count { it.status == Task.STATUS_COMPLETED }
    val totalTasks = tasks.size
    val percentage = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks

    // Calculate default emotional state based on real-time daily progress
    val defaultStatus = when {
        totalTasks == 0 -> MascotMessages.STATE_REMIND
        percentage == 1.0f -> MascotMessages.STATE_HAPPY
        percentage >= 0.5f -> MascotMessages.STATE_HAPPY
        else -> MascotMessages.STATE_SAD_PONTED
    }

    // Allow manual selection override to let the user "play with Bò Béo's emotions"
    var selectedStatusOverride by remember { mutableStateOf<String?>(null) }
    val currentStatus = selectedStatusOverride ?: defaultStatus

    var clickTrigger by remember { mutableStateOf(0) }
    val currentSpeechBubble = remember(currentStatus, clickTrigger) {
        MascotMessages.getNotificationContent(currentStatus)
    }

    var runBounceAnimation by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (runBounceAnimation) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { runBounceAnimation = false },
        label = "mascot_bounce"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(3.dp, shape = RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vườn cảm xúc Bò Béo 🐮",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (currentStatus) {
                                    MascotMessages.STATE_HAPPY -> com.example.ui.theme.SuccessMint
                                    MascotMessages.STATE_REMIND -> com.example.ui.theme.SecondaryPink
                                    MascotMessages.STATE_SAD_PONTED -> com.example.ui.theme.WarningPastel
                                    else -> com.example.ui.theme.ErrorCoral
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (currentStatus) {
                                MascotMessages.STATE_HAPPY -> "VUI VẺ"
                                MascotMessages.STATE_REMIND -> "NHẮC NHỞ"
                                MascotMessages.STATE_SAD_PONTED -> "BUỒN DỖI"
                                else -> "NỔI GIẬN"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.TextCharcoal
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onTriggerNotification(currentStatus)
                        runBounceAnimation = true
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Test Notification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scaleFactor)
                        .clickable {
                            clickTrigger++
                            runBounceAnimation = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.img_mascot_cow),
                        contentDescription = "Linh vật Bò Béo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (currentStatus) {
                                MascotMessages.STATE_HAPPY -> "🥰"
                                MascotMessages.STATE_REMIND -> "💡"
                                MascotMessages.STATE_SAD_PONTED -> "🥺"
                                else -> "😤"
                            },
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(
                            when (currentStatus) {
                                MascotMessages.STATE_HAPPY -> com.example.ui.theme.SuccessMint.copy(alpha = 0.3f)
                                MascotMessages.STATE_REMIND -> com.example.ui.theme.SecondaryPink.copy(alpha = 0.3f)
                                MascotMessages.STATE_SAD_PONTED -> com.example.ui.theme.WarningPastel.copy(alpha = 0.4f)
                                else -> com.example.ui.theme.ErrorCoral.copy(alpha = 0.3f)
                            }
                        )
                        .border(
                            1.dp,
                            when (currentStatus) {
                                MascotMessages.STATE_HAPPY -> com.example.ui.theme.SuccessMintBorder
                                MascotMessages.STATE_REMIND -> com.example.ui.theme.FocusPink
                                MascotMessages.STATE_SAD_PONTED -> com.example.ui.theme.WarningPastelBorder
                                else -> com.example.ui.theme.ErrorCoral.copy(alpha = 0.8f)
                            },
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentSpeechBubble,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = com.example.ui.theme.TextCharcoal,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thay đổi sắc thái Bò Béo:",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.TextGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(MascotMessages.STATE_HAPPY, "🥰 Vui", com.example.ui.theme.SuccessMint),
                    Triple(MascotMessages.STATE_REMIND, "💡 Nhắc", com.example.ui.theme.SecondaryPink),
                    Triple(MascotMessages.STATE_SAD_PONTED, "🥺 Dỗi", com.example.ui.theme.WarningPastel),
                    Triple(MascotMessages.STATE_ANGRY_ABANDONED, "😤 Giận", com.example.ui.theme.ErrorCoral)
                ).forEach { (state, label, bgColor) ->
                    val isActive = currentStatus == state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) bgColor else Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedStatusOverride = state
                                runBounceAnimation = true
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = com.example.ui.theme.TextCharcoal
                        )
                    }
                }
            }
        }
    }
}

