package com.tdminsight.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tdminsight.app.ui.theme.Slate500

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    hint: String? = null,
    error: String? = null,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { new -> onChange(new.filter { it.isDigit() || it == '.' || it == '-' }) },
            label = { Text(if (unit != null) "$label ($unit)" else label) },
            singleLine = true,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp, start = 4.dp))
        } else if (hint != null) {
            Text(hint, color = Slate500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp, start = 4.dp))
        }
    }
}
