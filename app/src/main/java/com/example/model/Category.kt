package com.example.model

import androidx.compose.ui.graphics.Color

data class Category(
    val id: Int,
    val name: String,
    val nameVi: String,
    val color: Color,
    val iconEmoji: String // Cute emoji for playful, beautiful aesthetics as requested!
) {
    companion object {
        val Categories = listOf(
            Category(1, "Work", "Công việc", Color(0xFFE0BBE4), "💼"), // PrimaryLavender
            Category(2, "Study", "Học tập", Color(0xFFFFD1DC), "📝"),  // SecondaryPink
            Category(3, "Personal", "Cá nhân", Color(0xFFFFFFD1), "🏠"), // WarningPastel
            Category(4, "Health", "Sức khỏe", Color(0xFFB5EAD7), "🌱")  // SuccessMint
        )

        fun getById(id: Int): Category {
            return Categories.find { it.id == id } ?: Categories[0]
        }
    }
}
