package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.M3BudgetCard
import com.example.ui.components.M3NoteCard
import com.example.ui.components.WeeklyTrendsBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    notes: List<NoteItem>,
    budgets: List<BudgetItem>,
    currencySymbol: String = "₹",
    onQuickAddClick: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onPinToggle: (NoteItem) -> Unit,
    onArchiveToggle: (NoteItem) -> Unit,
    onFavoriteToggle: (NoteItem) -> Unit,
    onLockToggle: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onDuplicateNote: (NoteItem) -> Unit,
    onShareNote: (NoteItem) -> Unit
) {
    val totalExpenses = remember(notes) {
        notes.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalIncome = remember(notes) {
        notes.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val overallBudget = remember(budgets) {
        budgets.firstOrNull { it.category == "All" }?.allocatedAmount ?: 25000.0
    }

    val recentNotes = remember(notes) {
        notes.take(5)
    }

    val topCategories = remember(notes) {
        notes.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.take(6)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notes Expenses",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Natural AI Financial Tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onQuickAddClick,
                    modifier = Modifier.testTag("dashboard_quick_add_header_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Quick AI Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Monthly Budget Card
        item {
            M3BudgetCard(
                allocatedBudget = overallBudget,
                spentAmount = totalExpenses,
                totalIncome = totalIncome,
                currencySymbol = currencySymbol,
                onManageBudgetsClick = onNavigateToBudgets
            )
        }

        // Top Spending Categories Row
        item {
            Column {
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (topCategories.isEmpty()) {
                    Text(
                        text = "No category data yet. Try adding notes!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(topCategories) { catEntry ->
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text("${catEntry.key}: $currencySymbol${catEntry.value.toInt()}")
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Weekly Trends Bar Chart
        item {
            WeeklyTrendsBarChart(
                notes = notes,
                currencySymbol = currencySymbol
            )
        }

        // Category Breakdown Pie Chart
        item {
            CategoryPieChart(
                notes = notes,
                currencySymbol = currencySymbol
            )
        }

        // Recent Notes Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToNotes) {
                    Text("View All (${notes.size}) >")
                }
            }
        }

        // Recent Notes Items
        if (recentNotes.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes recorded. Tap '+' or AI icon to write your first note!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentNotes, key = { it.id }) { note ->
                M3NoteCard(
                    note = note,
                    currencySymbol = currencySymbol,
                    onPinToggle = { onPinToggle(note) },
                    onArchiveToggle = { onArchiveToggle(note) },
                    onFavoriteToggle = { onFavoriteToggle(note) },
                    onLockToggle = { onLockToggle(note) },
                    onDelete = { onDeleteNote(note) },
                    onDuplicate = { onDuplicateNote(note) },
                    onShare = { onShareNote(note) },
                    onClick = { onNoteClick(note) }
                )
            }
        }
    }
}
