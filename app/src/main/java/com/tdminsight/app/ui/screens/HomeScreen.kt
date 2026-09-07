package com.tdminsight.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.ui.AppViewModel
import com.tdminsight.app.ui.components.DisclaimerBanner
import com.tdminsight.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNewCalculation: () -> Unit,
    onOpenHistoryEntry: (String) -> Unit,
) {
    val history by viewModel.history.collectAsState()
    val dateFmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("TDM Insight", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewCalculation, text = { Text("New calculation") }, icon = {})
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "Vancomycin Therapeutic Drug Monitoring, made explainable.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Slate900
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Select Pre, Post, or Pre+Post to run a dosing calculation with step-by-step results.",
                    color = Slate500,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item { DisclaimerBanner() }
            item {
                Text("Recent calculations", style = MaterialTheme.typography.titleMedium, color = Slate500)
            }
            if (history.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No calculations yet. Start a new one to see it here.", color = Slate500)
                    }
                }
            } else {
                items(history, key = { it.id }) { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                            .clickable { onOpenHistoryEntry(h.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(h.workflow.label, fontWeight = FontWeight.SemiBold, color = Slate900, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Dose ${h.result.inputs.dose.toInt()} mg · τ ${h.result.inputs.tau.toInt()}h · " +
                                    "Cmin ${"%.1f".format(h.result.pk["cmin"]?.value ?: 0.0)} mg/L · " +
                                    dateFmt.format(Date(h.timestamp)),
                                color = Slate500,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate500)
                    }
                }
                item {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear history")
                    }
                }
            }
            item {
                Text(
                    "CDE2313 · Mobile Application Development · Academic prototype — not for clinical use.",
                    color = Slate500,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
