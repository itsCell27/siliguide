package com.example.siliguide.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.OffWhite

/**
 * Standard reusable circular back button across SiliGuide screens.
 * Uses the custom chevron/arrow vector from the drawable resources.
 */
@Composable
fun SiliGuideBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.chevron_left,
    contentDescription: String = "Back",
    tint: Color = GrayBody,
    backgroundColor: Color = OffWhite,
    borderColor: Color = Border,
    size: Dp = 32.dp,
    iconSize: Dp = 16.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}
