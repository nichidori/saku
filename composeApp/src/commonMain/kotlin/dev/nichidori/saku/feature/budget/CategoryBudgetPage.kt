package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyBox
import dev.nichidori.saku.core.composable.MyIconButton
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Budget
import kotlinx.datetime.Month

@Composable
fun CategoryBudgetPage(
    viewModel: CategoryBudgetViewModel,
    onUp: () -> Unit,
    onDefaultBudgetClick: (String) -> Unit,
    onMonthBudgetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    LaunchedEffect(uiState.deleteStatus) {
        if (uiState.deleteStatus is Success) {
            onUp()
        }
    }

    Scaffold(
        topBar = {
            MyAppBar(
                title = uiState.template?.category?.name ?: "",
                onUp = onUp,
                action = {
                    MyIconButton(onClick = viewModel::delete) {
                        Icon(imageVector = Lucide.Trash2, contentDescription = "Delete Budget")
                    }
                }
            )
        },
        modifier = modifier
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(contentPadding).fillMaxSize()
        ) {
            uiState.template?.let { template ->
                item {
                    Text(
                        "Default Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MyBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDefaultBudgetClick(template.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Default", modifier = Modifier.weight(1f))
                            Text(
                                template.defaultAmount.toRupiah(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (uiState.budgets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Monthly Budgets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.budgets) { budget ->
                    MonthBudgetItem(
                        budget = budget,
                        onClick = { onMonthBudgetClick(budget.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthBudgetItem(
    budget: Budget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MyBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${Month(budget.month).name.take(3)} ${budget.year}",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    budget.baseAmount.toRupiah(),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Remaining",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    budget.remainingAmount.toRupiah(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (budget.remainingAmount < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
