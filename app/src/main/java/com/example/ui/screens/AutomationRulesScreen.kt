package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AutomationLog
import com.example.data.ai.AutomationRule
import com.example.data.ai.RuleAction
import com.example.data.ai.RuleCondition
import com.example.ui.viewmodel.NotesExpensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRulesScreen(
    viewModel: NotesExpensesViewModel,
    modifier: Modifier = Modifier
) {
    val rules by viewModel.automationRules.collectAsState()
    val logs by viewModel.automationLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("automation_rules_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Automation Engine", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_automation_rule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Smart Rule Engine Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Notes AI continuously evaluates your financial notes, budget thresholds, and bill schedules to execute triggers automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    "Active Automation Rules (${rules.count { it.isEnabled }}/${rules.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(rules, key = { it.id }) { rule ->
                AutomationRuleCard(
                    rule = rule,
                    onToggle = { viewModel.toggleAutomationRule(rule.id) },
                    onDelete = { viewModel.deleteAutomationRule(rule.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Recent Trigger History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        "No automation triggers recorded yet. Rules monitor incoming transactions in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    AutomationLogItem(log = log)
                }
            }
        }
    }

    if (showAddDialog) {
        AddAutomationRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, condition, action, value ->
                viewModel.addCustomAutomationRule(name, condition, action, value)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AutomationRuleCard(
    rule: AutomationRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("IF: ${rule.condition.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = { },
                        label = { Text("THEN: ${rule.action.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AutomationLogItem(log: AutomationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(log.ruleName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(log.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAutomationRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, RuleCondition, RuleAction, Double) -> Unit
) {
    var ruleName by remember { mutableStateOf("Custom Budget Rule") }
    var selectedCondition by remember { mutableStateOf(RuleCondition.BUDGET_EXCEEDS_PERCENT) }
    var selectedAction by remember { mutableStateOf(RuleAction.NOTIFY_USER) }
    var thresholdStr by remember { mutableStateOf("80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Automation Rule (IF → THEN)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("IF Condition:", style = MaterialTheme.typography.labelLarge)
                RuleCondition.values().forEach { cond ->
                    FilterChip(
                        selected = selectedCondition == cond,
                        onClick = { selectedCondition = cond },
                        label = { Text(cond.name.replace("_", " ")) }
                    )
                }

                Text("THEN Action:", style = MaterialTheme.typography.labelLarge)
                RuleAction.values().forEach { act ->
                    FilterChip(
                        selected = selectedAction == act,
                        onClick = { selectedAction = act },
                        label = { Text(act.name.replace("_", " ")) }
                    )
                }

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    label = { Text("Threshold / Limit Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(ruleName, selectedCondition, selectedAction, thresholdStr.toDoubleOrNull() ?: 80.0)
            }) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
