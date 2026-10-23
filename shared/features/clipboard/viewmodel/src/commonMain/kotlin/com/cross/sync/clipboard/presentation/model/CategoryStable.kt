package com.cross.sync.clipboard.presentation.model

import com.cross.sync.clipboard.domain.entity.Category

data class CategoryStable(
    val id: Long,
    val name: String,
) {
    companion object {
        val ALL = CategoryStable(
            id = -1,
            name = "All"
        )

        val UNCATEGORIZED = CategoryStable(
            id = -2,
            name = "Other"
        )
    }
}

fun Category.toStable(): CategoryStable = CategoryStable(id, name)

fun CategoryStable.toDomain(): Category = Category(id, name)
