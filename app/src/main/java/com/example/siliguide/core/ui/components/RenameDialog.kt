package com.example.siliguide.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
import com.example.siliguide.core.ui.theme.Fraunces
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.DEVICE_NAME_MAX_LENGTH

@Composable
fun RenameDialog(
    currentName: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
    renameCapital: String? = "Device",
    rename: String? = "device"
) {

    var name by remember(currentName) { mutableStateOf(currentName) }

    val trimmed = name.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != currentName

    Dialog(onDismissRequest = onCancel) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Rename $renameCapital",
                style = MaterialThemeHeadline,
                color = Black
            )

            Text(
                text = "Enter a new name for this $rename.",
                fontSize = 12.sp,
                fontFamily = DMSans,
                color = GrayBody,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(
                        width = 1.5.dp,
                        color = if (trimmed.isEmpty()) GreenLight else GreenPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Canvas(modifier = Modifier.size(13.dp)) {
                    drawDeviceGlyph(
                        color = GreenPrimary,
                        strokeWidthPx = 1.3.dp.toPx()
                    )
                }

                BasicTextField(
                    value = name,
                    onValueChange = { typed ->
                        if (typed.length <= DEVICE_NAME_MAX_LENGTH) name = typed
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "$renameCapital name",
                                    fontSize = 12.sp,
                                    fontFamily = DMSans,
                                    color = GrayHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = "${name.length}/$DEVICE_NAME_MAX_LENGTH",
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GreenPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OffWhite)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GrayBody
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) GreenPrimary else GreenLight)
                        .then(
                            if (canSave) {
                                Modifier.clickable { onConfirm(trimmed) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = White
                    )
                }
            }
        }
    }
}


/** Page title style — 18sp Fraunces (M3 headlineSmall in this theme). */
private val MaterialThemeHeadline = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = (-0.3).sp
)

private fun DrawScope.drawDeviceGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 22f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawRoundRect(
        color = color,
        topLeft = Offset(3f * s, 6f * s),
        size = androidx.compose.ui.geometry.Size(16f * s, 11f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.4f * s),
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(7f * s, 6f * s)
            lineTo(7f * s, 4.5f * s)
            quadraticTo(7f * s, 3.5f * s, 8f * s, 3.5f * s)
            lineTo(14f * s, 3.5f * s)
            quadraticTo(15f * s, 3.5f * s, 15f * s, 4.5f * s)
            lineTo(15f * s, 6f * s)
        },
        color = color,
        style = stroke
    )

    drawCircle(
        color = color,
        radius = 1.6f * s,
        center = Offset(11f * s, 11.5f * s)
    )
}