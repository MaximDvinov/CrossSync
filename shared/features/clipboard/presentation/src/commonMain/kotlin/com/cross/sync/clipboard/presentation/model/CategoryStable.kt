package com.cross.sync.clipboard.presentation.model

import androidx.compose.runtime.Stable
import com.cross.sync.clipboard.domain.entity.Category

@Stable
data class CategoryStable(
    val id: Long,
    val name: String,
) {
    companion object {
        val ALL = CategoryStable(
            id = -1,
            name = "All"
        )
    }
}

fun Category.toStable(): CategoryStable = CategoryStable(id, name)

fun CategoryStable.toDomain(): Category = Category(id, name)