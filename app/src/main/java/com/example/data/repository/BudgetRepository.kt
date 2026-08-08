package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.BudgetEntity
import com.example.data.model.BudgetItem
import com.example.data.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(private val budgetDao: BudgetDao) {

    val budgetsFlow: Flow<List<BudgetItem>> = budgetDao.getAllBudgets().map { list ->
        list.map { entityToModel(it) }
    }

    suspend fun saveBudget(budget: BudgetItem) {
        budgetDao.insertBudget(modelToEntity(budget))
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.deleteBudgetById(id)
    }

    private fun entityToModel(e: BudgetEntity): BudgetItem {
        return BudgetItem(
            id = e.id,
            name = e.name,
            category = e.category,
            allocatedAmount = e.allocatedAmount,
            spentAmount = e.spentAmount,
            period = try { BudgetPeriod.valueOf(e.period) } catch (err: Exception) { BudgetPeriod.MONTHLY },
            startDate = e.startDate,
            endDate = e.endDate
        )
    }

    private fun modelToEntity(m: BudgetItem): BudgetEntity {
        return BudgetEntity(
            id = m.id,
            name = m.name,
            category = m.category,
            allocatedAmount = m.allocatedAmount,
            spentAmount = m.spentAmount,
            period = m.period.name,
            startDate = m.startDate,
            endDate = m.endDate
        )
    }

    suspend fun seedDefaultBudgetsIfEmpty() {
        val defaults = listOf(
            BudgetItem("b1", "Monthly Overall Budget", "All", 25000.0, 0.0, BudgetPeriod.MONTHLY),
            BudgetItem("b2", "Groceries Budget", "Groceries", 6000.0, 0.0, BudgetPeriod.MONTHLY),
            BudgetItem("b3", "Dining Out & Food", "Food", 4000.0, 0.0, BudgetPeriod.MONTHLY)
        )
        defaults.forEach { saveBudget(it) }
    }
}
