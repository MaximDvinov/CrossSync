package com.cross.sync.setting.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cross.sync.clipboard.domain.entity.Category
import com.cross.sync.components.button.ButtonsDefaults
import com.cross.sync.components.button.RoundedIconButton
import com.cross.sync.theme.AppTheme
import com.cross.sync.theme.icons.Edit
import com.cross.sync.theme.icons.Plus
import com.cross.sync.theme.icons.Trash

@Composable
internal fun CategorySetting(
    state: SettingState,
    onDelete: (Category) -> Unit,
    onRename: (String, Category) -> Unit,
    onAddCategory: (String) -> Unit
) {
    val (newCategoryName, setCategoryName) = remember {
        mutableStateOf<String?>(null)
    }
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "Categories",
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.semiBold14.copy(color = AppTheme.colors.onSurface)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(color = AppTheme.colors.surface)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.categories.forEach { category ->
                CategoryItem(
                    modifier = Modifier,
                    category = category,
                    onDelete = {
                        onDelete(category)
                    },
                    onRename = {

                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(newCategoryName != null) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 8.dp,
                                    bottomStart = 8.dp,
                                    bottomEnd = 4.dp,
                                    topEnd = 4.dp
                                )
                            )
                            .background(AppTheme.colors.surfaceVariant)
                            .padding(horizontal = 12.dp)
                            .height(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = newCategoryName ?: "",
                            onValueChange = setCategoryName,
                            textStyle = AppTheme.typography.medium14.copy(AppTheme.colors.onSurfaceVariant),
                            modifier = Modifier.widthIn(min = 50.dp)
                        )
                    }
                }

                RoundedIconButton(
                    onClick = {
                        if (newCategoryName == null) {
                            setCategoryName("")
                        } else {
                            if (newCategoryName.isNotEmpty()) {
                                onAddCategory(newCategoryName)
                            }
                        }
                    },
                    modifier = Modifier,
                    colors = ButtonsDefaults.buttonPadding(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                        shape = if (newCategoryName == null) RoundedCornerShape(8.dp) else RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 8.dp,
                            topEnd = 8.dp
                        )
                    ),
                    imageVector = AppTheme.icons.Plus
                )
            }

        }
    }
}


@Composable
fun CategoryItem(
    modifier: Modifier,
    category: Category,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    Row(modifier = modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = category.name,
            style = AppTheme.typography.medium14.copy(AppTheme.colors.onSurfaceVariant),
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 4.dp,
                        topEnd = 4.dp
                    )
                )
                .background(AppTheme.colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        RoundedIconButton(
            onClick = {},
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonsDefaults.buttonPadding(
                vertical = 4.dp,
                horizontal = 8.dp,
                shape = RoundedCornerShape(4.dp)
            ),
            imageVector = AppTheme.icons.Edit
        )

        RoundedIconButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonsDefaults.buttonPadding(
                horizontal = 8.dp,
                vertical = 4.dp,
                shape = RoundedCornerShape(
                    bottomStart = 4.dp,
                    topStart = 4.dp,
                    bottomEnd = 8.dp,
                    topEnd = 8.dp
                ),
                containerColor = AppTheme.colors.redContainer,
                contentColor = AppTheme.colors.onRedContainer
            ),
            imageVector = AppTheme.icons.Trash
        )
    }
}