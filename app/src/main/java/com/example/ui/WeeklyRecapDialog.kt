package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyRecapDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onExportPdf: (List<Task>) -> Unit
) {
    // Selection state: map task ID to selected boolean
    var selectedTaskIds by remember { 
        mutableStateOf(tasks.map { it.id }.toSet()) 
    }

    // Stats calculations
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.status == Task.STATUS_COMPLETED }
    val percentage = if (totalTasks == 0) 0 else (completedTasks.toFloat() / totalTasks * 100).toInt()

    val evaluationMsg = when {
        totalTasks == 0 -> "Hãy bắt đầu tuần mới xinh tươi nhé! 🌸"
        percentage == 100 -> "Tuyệt vời tuyệt đối! Cậu đã hoàn thành tất cả! 🎉"
        percentage >= 80 -> "Xuất sắc rực rỡ! Tiến lên nào! 🌟"
        percentage >= 50 -> "Rất có tiến bộ! Bò Béo tự hào nè! 🌱"
        else -> "Cố lên một chút nữa nhé, Bò Béo luôn bên cạnh! 💪"
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
                    TopAppBar(
                        title = {
                            Text(
                                text = "Nhìn Lại Tuần Qua ✨",
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
                    
                    // --- SECTION 1: HEADER BANNER INFO ---
                    Text(
                        text = "Cùng tổng hợp những cột mốc hoàn thành rạng rỡ và xuất báo cáo PDF chuẩn mực ngay nào!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Start
                    )

                    // --- SECTION 2: AWESOME STATISTICS CIRCLE RADAR ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, shape = RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circular percentage indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(85.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryLavender.copy(alpha = 0.2f))
                                    .border(3.dp, PrimaryLavender, CircleShape)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$percentage%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextCharcoal
                                    )
                                    Text(
                                        text = "XONG",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray
                                    )
                                }
                            }

                            // Feedback message
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = evaluationMsg,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Đã hoàn thành $completedTasks trong tổng số $totalTasks công việc tuần này.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                // Custom miniature progress slider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryLavender.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks)
                                            .clip(CircleShape)
                                            .background(SuccessMintBorder)
                                    )
                                }
                            }
                        }
                    }

                    // --- SECTION 3: CHECKBOX LIST SPREADSHEEET ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Danh sách công việc của tuần này:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        
                        TextButton(
                            onClick = {
                                if (selectedTaskIds.size == tasks.size) {
                                    selectedTaskIds = emptySet()
                                } else {
                                    selectedTaskIds = tasks.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (selectedTaskIds.size == tasks.size) "Bỏ chọn hết" else "Chọn tất cả",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không tìm thấy công việc nào được lên lịch cho tuần hiện tại... ☕",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Sub Column of tasks
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            tasks.forEach { task ->
                                val isSelected = selectedTaskIds.contains(task.id)
                                val category = Category.getById(task.categoryId)
                                val isDone = task.status == Task.STATUS_COMPLETED

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTaskIds = if (isSelected) {
                                                selectedTaskIds - task.id
                                            } else {
                                                selectedTaskIds + task.id
                                            }
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryLavender else Color(0xFFEAEAEA),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PrimaryLavender.copy(alpha = 0.05f) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Category Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(category.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = category.iconEmoji, fontSize = 16.sp)
                                        }

                                        // Task details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDone) TextCharcoal.copy(alpha = 0.5f) else TextCharcoal,
                                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Hạn: ${task.reminderTime} | Trạng thái: ${if (isDone) "Hoàn thành" else "Chờ"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextGray
                                            )
                                        }

                                        // Custom Round Selection Checkbox
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PrimaryLavender else Color.Transparent)
                                                .border(2.dp, PrimaryLavender, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- SECTION 4: ACTIONS AND PDF BUTTON ---
                    Button(
                        onClick = {
                            val selectedTasksList = tasks.filter { selectedTaskIds.contains(it.id) }
                            onExportPdf(selectedTasksList)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(3.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "📊 Xuất Báo Cáo PDF Chuẩn Hoá",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
