package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType
import com.example.ui.components.AiSmartInsightsCard
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.WeeklyTrendsBarChart
import com.example.ui.theme.IncomeGreen

@Composable
fun AnalyticsScreen(
    notes: List<NoteItem>,
    budgets: List<BudgetItem> = emptyList(),
    currencySymbol: String = "₹",
    onApplyRecommendation: (String) -> Unit = {}
) {
    val expenses = remember(notes) { notes.filter { it.type == TransactionType.EXPENSE } }
    val income = remember(notes) { notes.filter { it.type == TransactionType.INCOME } }

    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val totalIncome = remember(income) { income.sumOf { it.amount } }
    val netSavings = (totalIncome - totalExpenses).coerceAtLeast(0.0)
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Analytics & AI Insights",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // AI Smart Insights
        AiSmartInsightsCard(
            notes = notes,
            budgets = budgets,
            currencySymbol = currencySymbol,
            onApplyRecommendation = onApplyRecommendation
        )

        // Savings Rate Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(32.dp))
                    Column {
                        Text(text = "Savings Rate", style = MaterialTheme.typography.labelMedium)
                        Text(text = "$savingsRate%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Net Saved", style = MaterialTheme.typography.labelSmall)
                    Text(text = "$currencySymbol${netSavings.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Charts
        WeeklyTrendsBarChart(notes = notes, currencySymbol = currencySymbol)

        CategoryPieChart(notes = notes, currencySymbol = currencySymbol)

        Spacer(modifier = Modifier.height(88.dp))
    }
}
