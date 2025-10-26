package dev.nichidori.saku.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import saku.composeapp.generated.resources.Poppins
import saku.composeapp.generated.resources.Res

@Composable
fun MyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = typography,
        content = content
    )
}

val typography: Typography
    @Composable
    get() = with(MaterialTheme.typography) {
        val poppins = FontFamily(Font(Res.font.Poppins))
        copy(
            displayLarge = displayLarge.copy(fontFamily = poppins),
            displayMedium = displayMedium.copy(fontFamily = poppins),
            displaySmall = displaySmall.copy(fontFamily = poppins),
            headlineLarge = headlineLarge.copy(fontFamily = poppins),
            headlineMedium = headlineMedium.copy(fontFamily = poppins),
            headlineSmall = headlineSmall.copy(fontFamily = poppins),
            titleLarge = titleLarge.copy(fontFamily = poppins),
            titleMedium = titleMedium.copy(fontFamily = poppins),
            titleSmall = titleSmall.copy(fontFamily = poppins),
            bodyLarge = bodyLarge.copy(fontFamily = poppins),
            bodyMedium = bodyMedium.copy(fontFamily = poppins),
            bodySmall = bodySmall.copy(fontFamily = poppins),
            labelLarge = labelLarge.copy(fontFamily = poppins),
            labelMedium = labelMedium.copy(fontFamily = poppins),
            labelSmall = labelSmall.copy(fontFamily = poppins)
        )
    }