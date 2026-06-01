package com.example.repository

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ParsedTask(
    @Json(name = "title") val title: String,
    @Json(name = "suggestedDate") val suggestedDate: String,
    @Json(name = "reason") val reason: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object AiTaskParser {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder().build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    suspend fun parseBrainDump(text: String, today: LocalDate = LocalDate.now()): List<ParsedTask> {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Return empty or fallback mock items if key is not available, avoiding crash
            return getLocalFallbackTasks(text, today)
        }

        val dayOfWeekVietnamese = when (today.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Thứ Hai"
            java.time.DayOfWeek.TUESDAY -> "Thứ Ba"
            java.time.DayOfWeek.WEDNESDAY -> "Thứ Tư"
            java.time.DayOfWeek.THURSDAY -> "Thứ Năm"
            java.time.DayOfWeek.FRIDAY -> "Thứ Sáu"
            java.time.DayOfWeek.SATURDAY -> "Thứ Bảy"
            java.time.DayOfWeek.SUNDAY -> "Chủ Nhật"
        }
        val dateFormatted = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val systemInstruction = """
            Bạn là trợ lý ảo lên lịch trình thông minh Lovable Scheduler. Nhiệm vụ của bạn là nhận các suy nghĩ tự do (Brain Dump) của người dùng về những việc họ đang nghĩ trong đầu, phân tích ý định, mức độ ưu tiên, thời gian và đề xuất sang danh sách các công việc cụ thể trong tuần này hoặc tuần sau.
            Hôm nay là $dayOfWeekVietnamese, ngày $dateFormatted. Hãy cực kỳ cẩn thận khi tính toán ngày 'suggestedDate' theo định dạng YYYY-MM-DD dựa trên thông tin này. Ví dụ: 'mai' nghĩa là ngày mai, 'cuối tuần này' nghĩa là Thứ Bảy hoặc Chủ Nhật tuần này, 'thứ năm tuần sau' nghĩa là Thứ Năm tiếp theo.
            Bạn BẮT BUỘC phản hồi bằng một mảng JSON duy nhất chứa định dạng sau, không bao gồm bất kỳ văn bản giải thích hoặc thẻ markdown nào bao quanh: 
            [
              {
                "title": "Tên công việc cụ thể đã được rút gọn và thêm icon sticker xinh xẻo ở đầu tên ví dụ ☕, 🌱, 💼, 📝, 🥦, 🏃‍♂️",
                "suggestedDate": "YYYY-MM-DD",
                "reason": "Lý do đề xuất ngày này ngắn gọn viết bằng giọng điệu dễ thương thân thiện tiếng Việt"
              }
            ]
        """.trimIndent()

        val fullPrompt = "Suy nghĩ tự do của người dùng: \"$text\"\n\nHãy phân tích và trả về lịch trình dạng JSON đúng ngữ pháp."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "$systemInstruction\n\n$fullPrompt")
                    )
                )
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse array JSON using Moshi
                val cleanJson = cleanJsonResponse(jsonText)
                val type = Types.newParameterizedType(List::class.java, ParsedTask::class.java)
                val adapter = moshi.adapter<List<ParsedTask>>(type)
                val rawList = adapter.fromJson(cleanJson) ?: emptyList()
                rawList.map { task ->
                    val sanitized = sanitizeDateStr(task.suggestedDate, today)
                    task.copy(suggestedDate = sanitized)
                }
            } else {
                getLocalFallbackTasks(text, today)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalFallbackTasks(text, today)
        }
    }

    private fun sanitizeDateStr(rawDate: String, fallback: LocalDate): String {
        val dateRegex = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
        val match = dateRegex.find(rawDate)
        if (match != null) {
            return match.value
        }
        return try {
            LocalDate.parse(rawDate.trim()).toString()
        } catch (e: Exception) {
            fallback.toString()
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun getLocalFallbackTasks(text: String, today: LocalDate): List<ParsedTask> {
        val list = mutableListOf<ParsedTask>()
        val lowercaseText = text.lowercase()
        
        if (lowercaseText.contains("học") || lowercaseText.contains("sách") || lowercaseText.contains("thi")) {
            list.add(
                ParsedTask(
                    title = "📝 Ôn tập bài vở học tập",
                    suggestedDate = today.toString(),
                    reason = "Lên lịch hôm nay để kịp tiến độ tự học của bạn nha!"
                )
            )
        }
        if (lowercaseText.contains("gym") || lowercaseText.contains("chạy") || lowercaseText.contains("khỏe") || lowercaseText.contains("tập")) {
            list.add(
                ParsedTask(
                    title = "🏃‍♂️ Tập thể dục rèn luyện thể thao",
                    suggestedDate = today.plusDays(1).toString(),
                    reason = "Ngày mai rèn luyện sức khỏe bền bỉ nè!"
                )
            )
        }
        if (lowercaseText.contains("mua") || lowercaseText.contains("chợ") || lowercaseText.contains("siêu thị")) {
            list.add(
                ParsedTask(
                    title = "🥦 Đi siêu thị sắm đồ dinh dưỡng",
                    suggestedDate = today.toString(),
                    reason = "Mua sắm hôm nay để chuẩn bị sẵn sàng thực phẩm ngon!"
                )
            )
        }
        if (lowercaseText.contains("gặp") || lowercaseText.contains("bạn") || lowercaseText.contains("cafe") || lowercaseText.contains("trà")) {
            list.add(
                ParsedTask(
                    title = "☕ Trò chuyện cafe thư giãn",
                    suggestedDate = today.plusDays(2).toString(),
                    reason = "Hẹn hò cuối tuần thong thả cùng tán gẫu xả stress nhé!"
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                ParsedTask(
                    title = "🌟 Lên lịch công việc cute mới",
                    suggestedDate = today.toString(),
                    reason = "Để hôm nay khởi đầu đầy hứng khởi nhé!"
                )
            )
        }
        return list
    }
}
