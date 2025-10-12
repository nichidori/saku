package dev.nichidori.saku.feature.categoryList

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyDefaultShape
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
            MyAppBar(title = "Categories", onUp = onUp)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCategoryClick,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Add category"
                )
            }
        },
        modifier = modifier,
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    shape = MyDefaultShape.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    ),
                    selected = uiState.selectedType == TrxType.Income,
                    onClick = { onSelectedTypeChange(TrxType.Income) },
                    icon = {},
                ) {
                    Text("Income", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    shape = MyDefaultShape.copy(
                        topStart = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp)
                    ),
                    selected = uiState.selectedType == TrxType.Expense,
                    onClick = { onSelectedTypeChange(TrxType.Expense) },
                    icon = {},
                ) {
                    Text("Expense", style = MaterialTheme.typography.labelMedium)
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                val categoriesByParent = if (uiState.selectedType == TrxType.Income) {
                    uiState.incomesByParent
                } else {
                    uiState.expensesByParent
                }
                categoriesByParent.forEach { (parent, children) ->
                    item {
                        CategoryCard(
                            category = parent,
                            onClick = onCategoryClick,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    itemsIndexed(children) { i, child ->
                        Row {
                            ChildNodeIndicator(
                                isLast = i == children.lastIndex,
                                modifier = Modifier.size(height = 64.dp, width = 48.dp)
                            )
                            CategoryCard(
                                category = child,
                                onClick = onCategoryClick,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .clickable { onClick(category.id) }
            .padding(16.dp),
    ) {
        Text(category.name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ChildNodeIndicator(
    modifier: Modifier = Modifier,
    isLast: Boolean = false
) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
        cap = StrokeCap.Round
    )
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = 0f),
            end = Offset(x = size.width / 2, y = size.height / 2),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
        drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = size.height / 2),
            end = Offset(x = size.width, y = size.height / 2),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
        if (!isLast) drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = size.height / 2),
            end = Offset(x = size.width / 2, y = size.height),
            strokeWidth = stroke.width,
            cap = stroke.cap,
            pathEffect = stroke.pathEffect
        )
    }
}
