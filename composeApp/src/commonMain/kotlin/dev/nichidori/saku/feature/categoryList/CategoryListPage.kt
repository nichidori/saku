package dev.nichidori.saku.feature.categoryList

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.domain.model.Category

@Composable
fun CategoryListPage(
    viewModel: CategoryListViewModel,
    onUp: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    CategoryListContent(
        uiState = uiState,
        onUp = onUp,
        onCategoryClick = onCategoryClick,
        modifier = modifier
    )
}

@Composable
fun CategoryListContent(
    uiState: CategoryListUiState,
    onUp: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MyAppBar(title = "Categories", onUp = onUp)
        },
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(contentPadding),
        ) {
            uiState.categoriesByParent.forEach { (parent, children) ->
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
