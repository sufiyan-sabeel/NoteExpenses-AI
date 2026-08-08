package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    budgets: List<BudgetItem>,
    notes: List<NoteItem>,
    categoriesList: List<String>,
    currencySymbol: String = "₹",
    onAddBudget: (name: String, category: String, amount: Double) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newBudgetName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var newBudgetAmount by remember { mutableStateOf("") }

    // Map spent amounts dynamically per budget category
    val updatedBudgets = remember(budgets, notes) {
        budgets.map { budget ->
            val spent = if (budget.category == "All") {
                notes.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            } else {
                notes.filter { it.type == TransactionType.EXPENSE && it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
            }
            budget.copy(spentAmount = spent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("budget_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Budgets & Limits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Track limits, warnings, and goal progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Budget")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (updatedBudgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No budgets configured. Tap 'New Budget' to create one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(updatedBudgets, key = { it.id }) { budget ->
                    val progress = if (budget.allocatedAmount > 0) (budget.spentAmount / budget.allocatedAmount).coerceIn(0.0, 1.0).toFloat() else 0f
                    val isExceeded = budget.spentAmount > budget.allocatedAmount && budget.allocatedAmount > 0
                    val remaining = budget.allocatedAmount - budget.spentAmount

                    val statusColor = when {
                        isExceeded -> ExpenseRed
                        progress > 0.85f -> WarningAmber
                        else -> IncomeGreen
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().testTag("budget_item_${budget.id}"),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = budget.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Category: ${budget.category} • ${budget.period.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { onDeleteBudget(budget.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: $currencySymbol${budget.spentAmount.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Text(
                                    text = "Limit: $currencySymbol${budget.allocatedAmount.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (remaining >= 0) "Remaining: $currencySymbol${remaining.toInt()}" else "Over budget by $currencySymbol${(-remaining).toInt()}!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Budget Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Create New Budget", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newBudgetName,
                        onValueChange = { newBudgetName = it },
                        label = { Text("Budget Name") },
                        placeholder = { Text("e.g. Monthly Dining Cap") },
                        modifier = Modifier.fillMaxWidth().testTag("add_budget_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newBudgetAmount,
                        onValueChange = { newBudgetAmount = it },
                        label = { Text("Allocated Amount ($currencySymbol)") },
                        placeholder = { Text("e.g. 5000") },
                        modifier = Modifier.fillMaxWidth().testTag("add_budget_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Category", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf("All") + categoriesList.take(6)).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newBudgetAmount.toDoubleOrNull() ?: 0.0
                        if (newBudgetName.isNotBlank() && amount > 0) {
                            onAddBudget(newBudgetName, selectedCategory, amount)
                            showAddDialog = false
                            newBudgetName = ""
                            newBudgetAmount = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_budget_btn")
                ) {
                    Text("Create Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
