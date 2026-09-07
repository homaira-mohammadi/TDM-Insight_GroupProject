package com.tdminsight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.ui.theme.Amber50
import com.tdminsight.app.ui.theme.Amber900

@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Amber50)
            .padding(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber900)
        Column {
            Text(
                "Academic prototype.",
                color = Amber900,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "For educational and software-development purposes only. Not a clinically " +
                    "validated prescribing or diagnostic system. All demonstration cases must be " +
                    "fictional. Verify all equations against lecturer-approved sources.",
                color = Amber900,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
