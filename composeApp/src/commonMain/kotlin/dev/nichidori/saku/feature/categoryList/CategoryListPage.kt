package dev.nichidori.saku.feature.categoryList

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.toPickerIcon
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType

@Composable
fun CategoryListPage(
    viewModel: CategoryListViewModel,
    onUp: () -> Unit,
    onNewCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    CategoryListContent(
        uiState = uiState,
        onUp = onUp,
        onSelectedTypeChange = viewModel::onSelectedTypeChange,
        onNewCategoryClick = onNewCategoryClick,
        onCategoryClick = onCategoryClick,
        modifier = modifier
    )
}

@Composable
fun CategoryListContent(
    uiState: CategoryListUiState,
    onUp: () -> Unit,
    onSelectedTypeChange: (TrxType) -> Unit,
    onNewCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MyAppBar(
                title = "Categories",
                onUp = onUp,
                action = {
                    MyIconButton(onClick = onNewCategoryClick) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "Add category"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            MySegmentedControl(
                items = listOf(TrxType.Income, TrxType.Expense),
                selectedItem = uiState.selectedType,
                onItemSelection = onSelectedTypeChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { type ->
                Text(
                    text = if (type == TrxType.Income) "Income" else "Expense",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            val categoriesByParent = if (uiState.selectedType == TrxType.Income) {
                uiState.incomesByParent
            } else {
                uiState.expensesByParent
            }
            if (categoriesByParent.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    categoriesByParent.onEachIndexed { i, (parent, children) ->
                        item {
                            CategoryCard(
                                category = parent,
                                onClick = onCategoryClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (i != 0) 16.dp else 0.dp)
                            )
                        }
                        itemsIndexed(children) { i, child ->
                            Row {
                                ChildNodeIndicator(
                                    isLast = i == children.lastIndex,
                                    yOffset = 8f,
                                    modifier = Modifier.size(height = (48 + 8).dp, width = 40.dp)
                                )
                                CategoryCard(
                                    category = child,
                                    onClick = onCategoryClick,
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                MyNoData(
                    message = "No categories yet",
                    contentDescription = "No categories",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                )
        ) {
            val icon = category.icon.toPickerIcon()?.icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    category.name.split(' ').take(2).joinToString("") {
                        it.firstOrNull()?.toString() ?: ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MyDefaultShape
                )
                .clip(MyDefaultShape)
                .clickable { onClick(category.id) }
                .padding(12.dp)
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ChildNodeIndicator(
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    yOffset: Float = 0f,
) {
    val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round)
    val color = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = 0f),
            end = Offset(x = size.width / 2, y = (size.height / 2) + yOffset),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
        drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = (size.height / 2) + yOffset),
            end = Offset(x = size.width, y = (size.height / 2) + yOffset),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
        if (!isLast) drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = (size.height / 2) + yOffset),
            end = Offset(x = size.width / 2, y = size.height),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
    }
}
