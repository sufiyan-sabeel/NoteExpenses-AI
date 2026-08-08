package com.example.data.ai

import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import java.util.UUID

enum class RuleCondition {
    BUDGET_EXCEEDS_PERCENT,
    RENT_DUE_SOON,
    RECURRING_BILL_DETECTED,
    DUPLICATE_NOTE_DETECTED,
    AUTO_CATEGORIZE_NOTE,
    HIGH_SINGLE_EXPENSE,
    AUTO_SYNC_SUPABASE
}

enum class RuleAction {
    NOTIFY_USER,
    CREATE_CALENDAR_REMINDER,
    SUGGEST_SAVINGS,
    AUTO_TAG_NOTE,
    TRIGGER_BACKUP
}

data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val condition: RuleCondition,
    val action: RuleAction,
    val isEnabled: Boolean = true,
    val thresholdValue: Double = 80.0,
    val targetCategory: String = "All",
    val lastTriggeredTime: Long = 0L,
    val triggerCount: Int = 0
)

data class AutomationLog(
    val id: String = UUID.randomUUID().toString(),
    val ruleName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AutomationEngine {

    private val _rules = mutableListOf<AutomationRule>()
    val rules: List<AutomationRule> get() = _rules.toList()

    private val _logs = mutableListOf<AutomationLog>()
    val logs: List<AutomationLog> get() = _logs.toList()

    init {
        // Seed default smart automation rules
        _rules.addAll(
            listOf(
                AutomationRule(
                    id = "rule_budget_80",
                    name = "Budget Exceeds 80% Alert",
                    condition = RuleCondition.BUDGET_EXCEEDS_PERCENT,
                    action = RuleAction.NOTIFY_USER,
                    thresholdValue = 80.0
                ),
                AutomationRule(
                    id = "rule_recurring_bill",
                    name = "Detect Recurring Rent/Bills",
                    condition = RuleCondition.RECURRING_BILL_DETECTED,
                    action = RuleAction.CREATE_CALENDAR_REMINDER
                ),
                AutomationRule(
                    id = "rule_duplicate",
                    name = "Duplicate Expense Detector",
                    condition = RuleCondition.DUPLICATE_NOTE_DETECTED,
                    action = RuleAction.NOTIFY_USER
                ),
                AutomationRule(
                    id = "rule_high_expense",
                    name = "High Single Expense Warning (>₹2,000)",
                    condition = RuleCondition.HIGH_SINGLE_EXPENSE,
                    action = RuleAction.SUGGEST_SAVINGS,
                    thresholdValue = 2000.0
                ),
                AutomationRule(
                    id = "rule_auto_tag",
                    name = "Auto-Tag Financial Notes",
                    condition = RuleCondition.AUTO_CATEGORIZE_NOTE,
                    action = RuleAction.AUTO_TAG_NOTE
                ),
                AutomationRule(
                    id = "rule_auto_backup",
                    name = "Auto Cloud Backup to Supabase",
                    condition = RuleCondition.AUTO_SYNC_SUPABASE,
                    action = RuleAction.TRIGGER_BACKUP
                )
            )
        )
    }

    fun toggleRule(ruleId: String) {
        val index = _rules.indexOfFirst { it.id == ruleId }
        if (index >= 0) {
            val updated = _rules[index].copy(isEnabled = !_rules[index].isEnabled)
            _rules[index] = updated
        }
    }

    fun addRule(rule: AutomationRule) {
        _rules.add(rule)
        _logs.add(AutomationLog(ruleName = rule.name, message = "Custom automation rule created."))
    }

    fun deleteRule(ruleId: String) {
        _rules.removeAll { it.id == ruleId }
    }

    /**
     * Evaluates active rules against newly added or modified notes and current budgets.
     * Returns a list of human-readable notification alerts triggered by automation.
     */
    fun evaluateNotesAndBudgets(
        notes: List<NoteItem>,
        budgets: List<BudgetItem>
    ): List<String> {
        val triggeredAlerts = mutableListOf<String>()

        _rules.filter { it.isEnabled }.forEach { rule ->
            when (rule.condition) {
                RuleCondition.BUDGET_EXCEEDS_PERCENT -> {
                    budgets.forEach { budget ->
                        if (budget.allocatedAmount > 0) {
                            val percentSpent = (budget.spentAmount / budget.allocatedAmount) * 100
                            if (percentSpent >= rule.thresholdValue) {
                                val msg = "⚠️ Automation Alert: '${budget.category}' budget is at ${percentSpent.toInt()}% (${budget.spentAmount}/${budget.allocatedAmount})!"
                                triggeredAlerts.add(msg)
                                logTrigger(rule.name, msg)
                            }
                        }
                    }
                }
                RuleCondition.DUPLICATE_NOTE_DETECTED -> {
                    if (notes.size >= 2) {
                        val latest = notes.firstOrNull()
                        if (latest != null) {
                            val duplicate = notes.drop(1).find {
                                it.amount == latest.amount &&
                                it.category.equals(latest.category, ignoreCase = true) &&
                                (latest.timestamp - it.timestamp) < 3600000 * 24 // within 24 hours
                            }
                            if (duplicate != null) {
                                val msg = "🔍 Duplicate Detected: You already added ₹${latest.amount} for ${latest.category} recently."
                                triggeredAlerts.add(msg)
                                logTrigger(rule.name, msg)
                            }
                        }
                    }
                }
                RuleCondition.HIGH_SINGLE_EXPENSE -> {
                    val recentHigh = notes.firstOrNull()
                    if (recentHigh != null && recentHigh.amount >= rule.thresholdValue) {
                        val msg = "💡 High Expense Alert: ₹${recentHigh.amount} spent on ${recentHigh.merchant}. AI recommends reviewing this budget item."
                        triggeredAlerts.add(msg)
                        logTrigger(rule.name, msg)
                    }
                }
                RuleCondition.RECURRING_BILL_DETECTED -> {
                    val recurringKeywords = listOf("rent", "electricity", "netflix", "wifi", "broadband", "bill", "emi")
                    notes.firstOrNull()?.let { note ->
                        if (recurringKeywords.any { note.rawText.lowercase().contains(it) }) {
                            val msg = "🗓️ Recurring Bill Detected: AI created a monthly calendar reminder for '${note.merchant}'."
                            triggeredAlerts.add(msg)
                            logTrigger(rule.name, msg)
                        }
                    }
                }
                else -> {}
            }
        }

        return triggeredAlerts.distinct()
    }

    private fun logTrigger(ruleName: String, msg: String) {
        _logs.add(0, AutomationLog(ruleName = ruleName, message = msg))
        if (_logs.size > 50) {
            _logs.removeAt(_logs.size - 1)
        }
    }
}
