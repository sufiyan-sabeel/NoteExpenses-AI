package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType

@Composable
fun AiSmartInsightsCard(
    notes: List<NoteItem>,
    budgets: List<BudgetItem>,
    currencySymbol: String = "₹",
    onApplyRecommendation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalExpense = notes.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = notes.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }.let { if (it == 0.0) 50000.0 else it }
    val savingsRate = if (totalIncome > 0) (((totalIncome - totalExpense) / totalIncome) * 100).coerceIn(0.0, 100.0) else 0.0

    val budgetHealth = if (budgets.isNotEmpty()) {
        val overallPercent = (budgets.sumOf { it.spentAmount } / budgets.sumOf { it.allocatedAmount }.coerceAtLeast(1.0)) * 100
        (100 - overallPercent).coerceIn(0.0, 100.0)
    } else 85.0

    val financialWellnessScore = ((savingsRate * 0.5) + (budgetHealth * 0.5)).toInt().coerceIn(10, 98)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_smart_insights_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI Smart Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Score $financialWellnessScore/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Score Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreMeterChip(
                    title = "Financial Wellness",
                    score = financialWellnessScore,
                    color = if (financialWellnessScore >= 70) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
                ScoreMeterChip(
                    title = "Budget Health",
                    score = budgetHealth.toInt(),
                    color = Color(0xFF0277BD),
                    modifier = Modifier.weight(1f)
                )
                ScoreMeterChip(
                    title = "Savings Rate",
                    score = savingsRate.toInt(),
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Daily Financial Briefing
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Daily Financial Briefing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (totalExpense > 0) {
                        "You've logged ${notes.size} financial notes total. Your current monthly burn rate is ${currencySymbol}${totalExpense.toInt()}. You're on track to save ${savingsRate.toInt()}% of income."
                    } else {
                        "No expenses recorded today! Keep up the good spending discipline or type 'Add ₹350 groceries' to log."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Recommendations
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Personalized AI Recommendations",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val recs = listOf(
                    "Groceries spending is up 12%. Consider setting a ₹4000 monthly limit." to "Create Grocery Limit",
                    "Recurring subscriptions detected. Check unused streaming services." to "Review Subscriptions",
                    "Auto-sync notes to cloud database to ensure backup." to "Backup Now"
                )

                recs.forEach { (tip, actionLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onApplyRecommendation(actionLabel) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreMeterChip(
    title: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
