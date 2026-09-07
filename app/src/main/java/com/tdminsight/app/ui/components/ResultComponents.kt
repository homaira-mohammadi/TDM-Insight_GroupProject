package com.tdminsight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.engine.ExplanationStep
import com.tdminsight.app.engine.ParamValue
import com.tdminsight.app.ui.theme.*

private fun fmtValue(v: Double): String = if (v.isNaN() || v.isInfinite()) "—" else "%.2f".format(v)

@Composable
fun ParamCard(label: String, value: Double, unit: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) Slate900 else Color.White)
            .border(1.dp, if (highlight) Slate900 else Slate200, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(label, color = if (highlight) Slate200 else Slate500, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmtValue(value),
                color = if (highlight) Color.White else Slate900,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(4.dp))
            Text(unit, color = if (highlight) Slate200 else Slate500, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ParamGrid(params: Map<String, ParamValue>, highlightKeys: Set<String> = emptySet(), modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        params.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, p) ->
                    ParamCard(
                        label = p.label,
                        value = p.value,
                        unit = p.unit,
                        highlight = key in highlightKeys,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ExplanationStepper(steps: List<ExplanationStep>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEach { s ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.dp, Slate200, CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.step, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Slate900)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .padding(12.dp)
                ) {
                    Text(s.label, fontWeight = FontWeight.SemiBold, color = Slate900, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(s.detail, color = Slate700, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(note: String, targetRange: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Emerald50)
            .padding(14.dp)
    ) {
        Text("Recommendation", fontWeight = FontWeight.SemiBold, color = Emerald600, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(note, color = Emerald600, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Target trough: $targetRange · AUC24 target: 400–600 mg·h/L", color = Emerald600, style = MaterialTheme.typography.labelSmall)
    }
}
