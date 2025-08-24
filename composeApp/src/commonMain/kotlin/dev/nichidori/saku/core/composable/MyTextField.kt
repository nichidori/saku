package dev.nichidori.saku.core.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape

@Suppress("ModifierParameter")
@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        shape = RectangleShape,
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .height(TextFieldDefaults.MinHeight)
    )
}