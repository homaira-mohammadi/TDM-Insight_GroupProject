package com.tdminsight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdminsight.app.engine.Workflow
import com.tdminsight.app.ui.theme.Slate200
import com.tdminsight.app.ui.theme.Slate500
import com.tdminsight.app.ui.theme.Slate900

@Composable
fun WorkflowSelector(
    selected: Workflow?,
    onSelect: (Workflow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Workflow.entries.forEach { w ->
            val active = selected == w
            val icon = when (w) {
                Workflow.PRE -> Icons.Filled.ArrowDownward
                Workflow.POST -> Icons.Filled.ArrowUpward
                Workflow.PRE_POST -> Icons.Filled.CallMerge
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) Slate900 else Color.White)
                    .border(1.dp, if (active) Slate900 else Slate200, RoundedCornerShape(16.dp))
                    .clickable { onSelect(w) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = if (active) Color.White else Slate900)
                    Spacer(Modifier.width(8.dp))
                    Text(w.short, color = if (active) Color.White else Slate900, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    w.description,
                    color = if (active) Slate200 else Slate500,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
