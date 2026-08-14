package com.kitsunetech.sweep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kitsunetech.sweep.domain.toReadableBytes
import com.kitsunetech.sweep.ui.theme.ColdMint
import com.kitsunetech.sweep.ui.theme.ByteValueStyle
import com.kitsunetech.sweep.ui.theme.Mist
import com.kitsunetech.sweep.ui.theme.WarningClay

@Composable
fun StorageStrata(
    totalBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier,
) {
    val total = totalBytes.coerceAtLeast(0L)
    val free = freeBytes.coerceIn(0L, total.takeIf { it > 0L } ?: 0L)
    val used = (total - free).coerceAtLeast(0L)
    val usedWeight = if (total > 0L) used.toFloat().coerceAtLeast(0.001f) else 1f
    val freeWeight = if (total > 0L) free.toFloat().coerceAtLeast(0.001f) else 0.001f

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp)),
        ) {
            Box(Modifier.weight(usedWeight).height(18.dp).background(WarningClay))
            Box(Modifier.weight(freeWeight).height(18.dp).background(ColdMint))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StrataLabel(color = WarningClay, label = "Used", value = used.toReadableBytes())
            StrataLabel(color = ColdMint, label = "Free", value = free.toReadableBytes())
        }
        if (total > 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(total.toReadableBytes(), style = ByteValueStyle, color = Mist)
                Text("total", style = MaterialTheme.typography.bodyMedium, color = Mist)
            }
        } else {
            Text("Storage total unavailable", style = MaterialTheme.typography.bodyMedium, color = Mist)
        }
    }
}

@Composable
private fun StrataLabel(color: Color, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = ByteValueStyle)
    }
}
