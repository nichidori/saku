package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyBox
import dev.nichidori.saku.core.composable.MyIconButton
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.status
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames

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
        when (val status = uiState.deleteStatus) {
            is Success<*> -> onUp()
            is Status.Failure<*> -> showToast(
                status.error.toString(),
                duration = ToastDuration.Long
            )

            else -> {}
        }
    }

    CategoryBudgetPageContent(
        uiState = uiState,
        onUp = onUp,
        onDelete = viewModel::delete,
        onDefaultBudgetClick = onDefaultBudgetClick,
        onMonthBudgetClick = onMonthBudgetClick,
        modifier = modifier
    )
}

@Composable
fun CategoryBudgetPageContent(
    uiState: CategoryBudgetUiState,
    onUp: () -> Unit,
    onDelete: () -> Unit,
    onDefaultBudgetClick: (String) -> Unit,
    onMonthBudgetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MyAppBar(
                title = "Budget",
                onUp = onUp,
                action = {
                    MyIconButton(
                        content = {
                            Icon(
                                imageVector = Lucide.Trash,
                                contentDescription = "Delete budget",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onDelete,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        },
        modifier = modifier
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(contentPadding).fillMaxSize()
        ) {
            uiState.template?.let { template ->
                item {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        template.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MyBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDefaultBudgetClick(template.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Default",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                template.defaultAmount.toRupiah(),
                                style = MaterialTheme.typography.bodyMedium,
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
                        "Monthly Budget",
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
            .background(
                color = if (budget.status.isActive) MaterialTheme.colorScheme.secondary
                else Color.Transparent
            )
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            val date = LocalDate(
                year = budget.year,
                month = budget.month,
                day = 1
            )
            val month = date.format(LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) })
            val year = (date.year % 100).toString().padStart(2, '0')

            Text(
                "$month $year",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                budget.baseAmount.toRupiah(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
