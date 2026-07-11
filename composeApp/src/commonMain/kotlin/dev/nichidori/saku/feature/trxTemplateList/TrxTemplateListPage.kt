package dev.nichidori.saku.feature.trxTemplateList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.TrxTemplate
import dev.nichidori.saku.domain.model.TrxType

@Composable
fun TrxTemplateListPage(
    viewModel: TrxTemplateListViewModel,
    onUp: () -> Unit,
    onNewTemplateClick: (TrxType) -> Unit,
    onTemplateClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    TrxTemplateListContent(
        uiState = uiState,
        onUp = onUp,
        onSelectedTypeChange = viewModel::onSelectedTypeChange,
        onNewTemplateClick = onNewTemplateClick,
        onTemplateClick = onTemplateClick,
        modifier = modifier
    )
}

@Composable
fun TrxTemplateListContent(
    uiState: TrxTemplateListUiState,
    onUp: () -> Unit,
    onSelectedTypeChange: (TrxType) -> Unit,
    onNewTemplateClick: (TrxType) -> Unit,
    onTemplateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MyAppBar(
                title = "Templates",
                onUp = onUp,
                action = {
                    MyIconButton(
                        onClick = { onNewTemplateClick(uiState.selectedType) }
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "Add template"
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
                items = TrxType.entries,
                selectedItem = uiState.selectedType,
                onItemSelection = onSelectedTypeChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { type ->
                Text(
                    text = when (type) {
                        TrxType.Income -> "Income"
                        TrxType.Expense -> "Expense"
                        TrxType.Transfer -> "Transfer"
                    }
                )
            }
            val filtered = uiState.filteredTemplates
            if (filtered.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    modifier = Modifier.weight(1f).padding(top = 16.dp),
                ) {
                    itemsIndexed(filtered) { index, template ->
                        TrxTemplateCard(
                            template = template,
                            onClick = { onTemplateClick(template.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (index < filtered.lastIndex) 12.dp else 0.dp)
                        )
                    }
                }
            } else {
                MyNoData(
                    message = "No templates yet",
                    contentDescription = "No templates",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TrxTemplateCard(
    template: TrxTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MyBox(
        modifier = modifier
            .clip(MyDefaultShape)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                template.amount.toRupiah(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
