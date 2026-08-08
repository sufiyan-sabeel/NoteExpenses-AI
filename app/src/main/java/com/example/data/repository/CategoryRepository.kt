package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.model.CategoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    val defaultCategories = listOf(
        CategoryItem("cat_1", "Groceries", "shopping_cart", "#2E7D32"),
        CategoryItem("cat_2", "Utilities", "electric_bolt", "#D84315"),
        CategoryItem("cat_3", "Transport", "directions_bus", "#0277BD"),
        CategoryItem("cat_4", "Fuel", "local_gas_station", "#E65100"),
        CategoryItem("cat_5", "Shopping", "checkroom", "#8E24AA"),
        CategoryItem("cat_6", "Medical", "medical_services", "#C62828"),
        CategoryItem("cat_7", "Rent", "home", "#455A64"),
        CategoryItem("cat_8", "Food", "restaurant", "#F57C00"),
        CategoryItem("cat_9", "Entertainment", "movie", "#6A1B9A"),
        CategoryItem("cat_10", "Education", "school", "#1565C0"),
        CategoryItem("cat_11", "Travel", "flight", "#00838F"),
        CategoryItem("cat_12", "Investment", "trending_up", "#00695C"),
        CategoryItem("cat_13", "Income", "account_balance_wallet", "#1B5E20"),
        CategoryItem("cat_14", "Savings", "savings", "#004D40"),
        CategoryItem("cat_15", "Others", "more_horiz", "#3F51B5")
    )

    val categoriesFlow: Flow<List<CategoryItem>> = categoryDao.getAllCategories().map { entities ->
        if (entities.isEmpty()) {
            defaultCategories
        } else {
            entities.map {
                CategoryItem(
                    id = it.id,
                    name = it.name,
                    iconName = it.iconName,
                    colorHex = it.colorHex,
                    isCustom = it.isCustom,
                    budgetLimit = it.budgetLimit
                )
            }
        }
    }

    suspend fun seedDefaultsIfEmpty() {
        val list = defaultCategories.map {
            CategoryEntity(
                id = it.id,
                name = it.name,
                iconName = it.iconName,
                colorHex = it.colorHex,
                isCustom = it.isCustom,
                budgetLimit = it.budgetLimit
            )
        }
        categoryDao.insertCategories(list)
    }

    suspend fun addCategory(category: CategoryItem) {
        val entity = CategoryEntity(
            id = category.id,
            name = category.name,
            iconName = category.iconName,
            colorHex = category.colorHex,
            isCustom = category.isCustom,
            budgetLimit = category.budgetLimit
        )
        categoryDao.insertCategory(entity)
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
    }
}
