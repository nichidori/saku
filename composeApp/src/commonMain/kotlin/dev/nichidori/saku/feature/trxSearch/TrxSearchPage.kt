package dev.nichidori.saku.feature.trxSearch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import dev.nichidori.saku.core.composable.MyIconButton
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.composable.MyTextField
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.feature.trxList.TrxListContent
import dev.nichidori.saku.feature.trxList.TrxListUiState

@Composable
fun TrxSearchPage(
    viewModel: TrxSearchViewModel,
    onUp: () -> Unit,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    val query by viewModel.query.collectAsStateWithLifecycleIfAvailable()
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues()
                            .calculateTopPadding()
                    )
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 12.dp)
            ) {
                MyIconButton(onClick = onUp) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                MyTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    label = "",
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = viewModel::clearQuery) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.weight(1f).focusRequester(searchFocusRequester).padding(bottom = 8.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Box(
            modifier = Modifier.padding(contentPadding).fillMaxSize()
        ) {
            if (query.isBlank()) {
                MyNoData(
                    message = "Type to search your transactions",
                    contentDescription = "Search transactions",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TrxListContent(
                    uiState = TrxListUiState.MonthlyState(
                        loadStatus = uiState.status,
                        trxRecordsByDate = uiState.recordsByDate,
                    ),
                    onTrxClick = onTrxClick,
                    emptyMessage = "No results found",
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                )
            }
        }
    }
}
