package dev.nichidori.saku.feature.categoryList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(contentPadding),
        ) {
            uiState.categoriesByParent.forEach { (parent, children) ->
                item {
                    CategoryCard(
                        category = parent,
                        onClick = onCategoryClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(children) { child ->
                    CategoryCard(
                        category = child,
                        onClick = onCategoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    )
                }
            }

        }
    }
}

@Composable
fun CategoryCard(category: Category, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = if (category.parent != null) 16.dp else 0.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .clickable { onClick(category.id) }
            .padding(16.dp),
    ) {
        Text(category.name, style = MaterialTheme.typography.labelSmall)
    }
}