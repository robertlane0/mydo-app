package com.mydo.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mydo.app.domain.model.Priority

val MydoRed = Color(0xFFD1453B)
val MydoPriority2 = Color(0xFFEB8909)
val MydoPriority3 = Color(0xFF246FE0)
val MydoPriority4 = Color(0xFF8A8A8A)
val MydoDarkBackground = Color(0xFF1E1E1E)

@Immutable
data class MydoPriorityColors(
    val p1: Color = MydoRed,
    val p2: Color = MydoPriority2,
    val p3: Color = MydoPriority3,
    val p4: Color = MydoPriority4,
) {
    fun colorFor(priority: Priority): Color {
        return when (priority) {
            Priority.P1 -> p1
            Priority.P2 -> p2
            Priority.P3 -> p3
            Priority.P4 -> p4
        }
    }
}

val LocalPriorityColors = compositionLocalOf { MydoPriorityColors() }
