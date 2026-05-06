package com.example.siliguide.presentation.features.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.GrayMedium

@Composable
fun SectionTag(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        color = GrayMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )
}