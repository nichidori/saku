package org.arraflydori.fin.core.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus

@Composable
fun MyNavBar(
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onStatisticClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, Color.LightGray)
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(
            imageVector = Lucide.House,
            contentDescription = "Home",
            modifier = Modifier
                .size(48.dp)
                .clip(MyDefaultShape)
                .clickable { onHomeClick() }
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = Color.LightGray, shape = MyDefaultShape)
                .clip(MyDefaultShape)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Lucide.Plus, contentDescription = "Add")
        }
        Spacer(modifier = Modifier.width(32.dp))
        Icon(
            imageVector = Lucide.ChartPie,
            contentDescription = "Statistic",
            modifier = Modifier
                .size(48.dp)
                .clip(MyDefaultShape)
                .clickable { onStatisticClick() }
                .padding(12.dp)
        )
    }
}