package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.repository.AiTaskParser
import com.example.repository.ParsedTask
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BrainDumpTriggerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("brain_dump_trigger_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val stroke = Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = PrimaryLavender.copy(alpha = 0.5f),
                        style = stroke,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Shiny animated background frame for the Brain icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(SecondaryPink, PrimaryLavender)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 26.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Góc Xả Não Thông Minh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        Text(
                            text = "AI ✨",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = FocusPink,
                            modifier = Modifier
                                .background(SecondaryPink.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bạn đang nghĩ nhiều việc trong đầu? Viết tự do vào đây để AI phân tích và tự lên lịch xinh xắn nha! 💕",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextLightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainDumpDialog(
    onDismiss: () -> Unit,
    onSaveTasks: (tasks: List<ParsedTask>) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var parsedSuggestions by remember { mutableStateOf<List<ParsedTask>?>(null) }
    var selectedItems by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, PrimaryLavender, RoundedCornerShape(24.dp)),
            color = BackgroundCream
        ) {
            Scaffold(
                containerColor = BackgroundCream,
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = BackgroundCream
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🧠 Góc Xả Não Viết Nốt",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextCharcoal
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = FocusPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, enabled = !isLoading) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = TextCharcoal
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tip banner mimicking cute instructions
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryPink.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Viết tự do những việc đang lộn xộn trong đầu bạn ví dụ 'mai đi siêu thị sắm đồ, chủ nhật lượn lờ trà sữa lúc 15h, thứ 2 tuần sau làm bài tập...'. AI sẽ tính toán và đề xuất lịch!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextCharcoal.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Multi-line cute ruled notepad
                    Text(
                        text = "Trang sổ tay ghi nhớ của bạn 📒",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFFDF5)) // Notebook off-white warm cream
                            .border(1.5.dp, Color(0xFFE8DFCD), RoundedCornerShape(16.dp))
                            .drawBehind {
                                val lineColor = Color(0xFFE2EAFD)
                                val marginColor = Color(0xFFFFC0CB)

                                // Notebook rows rules
                                val ruleSpacing = 32.dp.toPx()
                                val totalLines = (size.height / ruleSpacing).toInt()
                                for (i in 1..totalLines) {
                                    val rowY = i * ruleSpacing
                                    drawLine(
                                        color = lineColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, rowY),
                                        end = androidx.compose.ui.geometry.Offset(size.width, rowY),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Cute red left margin line
                                val marginX = 42.dp.toPx()
                                drawLine(
                                    color = marginColor,
                                    start = androidx.compose.ui.geometry.Offset(marginX, 0f),
                                    end = androidx.compose.ui.geometry.Offset(marginX, size.height),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                    ) {
                        BasicTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 52.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                                .testTag("brain_dump_text_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = TextCharcoal,
                                lineHeight = 32.sp // Aligns nicely with the lines drawn at 32.dp spacing!
                            ),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (rawText.isEmpty()) {
                                        Text(
                                            text = "Viết suy nghĩ lộn xộn ở đây...\nVí dụ:\n- Mai đi chợ mua râu tây tầm 9 giờ sáng nha.\n- Chủ nhật rảnh rỗi đi tụ tập cafe với nhóm bạn đại học lúc 16h.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Gray.copy(alpha = 0.5f),
                                                lineHeight = 32.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // Magic Submit Button
                    Button(
                        onClick = {
                            if (rawText.trim().isNotEmpty()) {
                                isLoading = true
                                errorMessage = null
                                parsedSuggestions = null
                                coroutineScope.launch {
                                    try {
                                        // Pass the actual current date 
                                        val results = AiTaskParser.parseBrainDump(rawText)
                                        parsedSuggestions = results
                                        // Auto select all by default
                                        selectedItems = results.mapIndexed { index, _ -> index to true }.toMap()
                                    } catch (e: Exception) {
                                        errorMessage = "Không thể kết nối AI, vui lòng thử lại nhé!"
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && rawText.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .testTag("ai_arrange_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // PrimaryLavender
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nhờ AI sắp xếp hộ ✨",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Progress Loader / Success Results Display
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FocusPink,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Chú bò cute đang xả não hộ bạn... Vui lòng chờ xíu xiu...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Suggestions layout
                    parsedSuggestions?.let { suggestions ->
                        Text(
                            text = "Lịch trình đề xuất cho bạn 🗓️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )

                        if (suggestions.isEmpty()) {
                            Text(
                                text = "Không tìm thấy công việc cụ thể hay ngày giờ trong ghi chú của bạn. Hãy ghi chi tiết hơn xem sao nha!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            suggestions.forEachIndexed { index, item ->
                                val isChecked = selectedItems[index] ?: true
                                val parsedDate = try {
                                    LocalDate.parse(item.suggestedDate)
                                } catch (e: Exception) {
                                    LocalDate.now()
                                }
                                val formatter = DateTimeFormatter.ofPattern("EEEE, dd/MM", java.util.Locale.forLanguageTag("vi"))
                                val dateLabel = parsedDate.format(formatter).replaceFirstChar { it.uppercase() }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isChecked) 1.5.dp else 1.dp,
                                            color = if (isChecked) PrimaryLavender else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { selected ->
                                                selectedItems = selectedItems.toMutableMap().apply {
                                                    put(index, selected)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PrimaryLavender,
                                                checkmarkColor = Color.White
                                            )
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChecked) TextCharcoal else TextGray
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = FocusPink,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = dateLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isChecked) MaterialTheme.colorScheme.primary else TextGray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChecked) TextGray else TextLightGray
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val acceptedTasks = suggestions.filterIndexed { idx, _ -> selectedItems[idx] ?: true }
                                    if (acceptedTasks.isNotEmpty()) {
                                        onSaveTasks(acceptedTasks)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .shadow(2.dp, RoundedCornerShape(14.dp))
                                    .testTag("save_ai_tasks_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FocusPink,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Thêm toàn bộ vào lịch 🌸",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
