package com.cross.sync.core.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation

@Entity(primaryKeys = ["categoryId", "copiedDataId"])
data class CategoryCopiedDataCrossRef(
    val categoryId: Long,
    val copiedDataId: Long,
)

@Entity
data class CategoryWithCopiedData(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "copiedDataId",
        associateBy = Junction(CategoryCopiedDataCrossRef::class),
    )
    val copiedDataList: List<CopiedDataEntity>,
)

@Entity
data class CopiedDataWithCategories(
    @Embedded val copiedData: CopiedDataEntity,
    @Relation(
        parentColumn = "copiedDataId",
        entityColumn = "categoryId",
        associateBy = Junction(CategoryCopiedDataCrossRef::class)
    )
    val categoryList: List<CategoryEntity>,
)