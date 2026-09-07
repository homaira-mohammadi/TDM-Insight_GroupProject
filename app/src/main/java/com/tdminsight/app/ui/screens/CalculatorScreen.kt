package com.tdminsight.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.engine.TdmFormInputs
import com.tdminsight.app.engine.Workflow
import com.tdminsight.app.ui.AppViewModel
import com.tdminsight.app.ui.components.NumberField
import com.tdminsight.app.ui.components.WorkflowSelector
import com.tdminsight.app.ui.theme.Slate500
import com.tdminsight.app.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onCalculated: () -> Unit,
) {
    var workflow by remember { mutableStateOf<Workflow?>(null) }

    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<String?>(null) }
    var scr by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var tau by remember { mutableStateOf("") }
    var tInf by remember { mutableStateOf("") }
    var cmin by remember { mutableStateOf("") }
    var cpeak by remember { mutableStateOf("") }
    var tPeak by remember { mutableStateOf("") }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New calculation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("1. Select workflow", fontWeight = FontWeight.SemiBold, color = Slate900)
            Spacer(Modifier.height(8.dp))
            WorkflowSelector(selected = workflow, onSelect = { workflow = it })

            if (workflow != null) {
                Spacer(Modifier.height(20.dp))
                Text("2. Patient & dosing parameters", fontWeight = FontWeight.SemiBold, color = Slate900)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Age", age, { age = it }, unit = "years", modifier = Modifier.weight(1f))
                    NumberField("Weight", weight, { weight = it }, unit = "kg", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))

                Text("Sex", color = Slate500, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sex == "male", onClick = { sex = "male" }, label = { Text("Male") })
                    FilterChip(selected = sex == "female", onClick = { sex = "female" }, label = { Text("Female") })
                }
                Spacer(Modifier.height(10.dp))

                NumberField("Serum creatinine", scr, { scr = it }, unit = "mg/dL")
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Dose", dose, { dose = it }, unit = "mg", modifier = Modifier.weight(1f))
                    NumberField("Interval (τ)", tau, { tau = it }, unit = "h", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                NumberField("Infusion duration", tInf, { tInf = it }, unit = "h", hint = "Must be shorter than the dosing interval")

                Spacer(Modifier.height(20.dp))
                Text("3. Sampling & concentration", fontWeight = FontWeight.SemiBold, color = Slate900)
                Spacer(Modifier.height(8.dp))

                when (workflow) {
                    Workflow.PRE -> {
                        NumberField("Measured trough (Cmin)", cmin, { cmin = it }, unit = "mg/L")
                    }
                    Workflow.POST -> {
                        NumberField("Measured peak (Cmax)", cpeak, { cpeak = it }, unit = "mg/L")
                        Spacer(Modifier.height(10.dp))
                        NumberField("Time to peak sample", tPeak, { tPeak = it }, unit = "h after end of infusion")
                    }
                    Workflow.PRE_POST -> {
                        NumberField("Measured trough (Cmin)", cmin, { cmin = it }, unit = "mg/L")
                        Spacer(Modifier.height(10.dp))
                        NumberField("Measured peak (Cmax)", cpeak, { cpeak = it }, unit = "mg/L")
                        Spacer(Modifier.height(10.dp))
                        NumberField("Time to peak sample", tPeak, { tPeak = it }, unit = "h after end of infusion")
                    }
                    null -> {}
                }

                if (errors.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            errors.forEach { e ->
                                Text("• $e", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val form = TdmFormInputs(
                            age = age, weight = weight, sex = sex, scr = scr,
                            dose = dose, tau = tau, tInf = tInf,
                            cminMeasured = cmin, cpeakMeasured = cpeak, tPeak = tPeak,
                        )
                        val result = viewModel.calculate(workflow!!, form)
                        if (result.isEmpty()) {
                            errors = emptyList()
                            onCalculated()
                        } else {
                            errors = result
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run calculation")
                }
                Spacer(Modifier.height(40.dp))
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
