package dev.nichidori.saku.feature.trxTemplateList

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.TrxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrxTemplateListBottomSheet(
    viewModel: TrxTemplateListBottomSheetViewModel,
    onDismissRequest: () -> Unit,
    onTrxCreated: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingTemplateId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(pendingTemplateId) {
        pendingTemplateId?.let { templateId ->
            val newTrxId = viewModel.createTrxFromTemplate(templateId)
            pendingTemplateId = null
            onTrxCreated(newTrxId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        TrxTemplateListBottomSheetContent(
            templates = uiState.templates,
            onTemplateClick = { templateId ->
                pendingTemplateId = templateId
            },
        )
    }
}

@Composable
private fun TrxTemplateListBottomSheetContent(
    templates: List<dev.nichidori.saku.domain.model.TrxTemplate>,
    onTemplateClick: (String) -> Unit,
) {
    val grouped = TrxType.entries.mapNotNull { type ->
        val items = templates.filter { it.type == type }
        if (items.isNotEmpty()) type to items else null
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        grouped.forEach { (type, items) ->
            item(key = "header_${type.name}") {
                Text(
                    text = when (type) {
                        TrxType.Income -> "Income"
                        TrxType.Expense -> "Expense"
                        TrxType.Transfer -> "Transfer"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
            itemsIndexed(items, key = { _, item -> item.id }) { index, template ->
                TrxTemplateCard(
                    template = template,
                    onClick = { onTemplateClick(template.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < items.lastIndex) 8.dp else 0.dp)
                )
            }
        }
    }
}
