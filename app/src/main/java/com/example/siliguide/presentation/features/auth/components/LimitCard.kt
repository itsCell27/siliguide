package com.example.siliguide.presentation.features.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.OffWhite

@Composable
fun LimitCard(
    items: List<Pair<String, String>>,
    isPositive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
    ) {

        items.forEachIndexed { index, item ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            if (isPositive) GreenLight else Color(0xFFFCEBEB),
                            RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isPositive) "✓" else "✕")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        item.first,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isPositive) GreenDark else Black
                    )
                    Text(
                        item.second,
                        fontSize = 10.sp,
                        color = GrayMedium
                    )
                }
            }

            if (index != items.lastIndex) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Border)
            }
        }
    }
}