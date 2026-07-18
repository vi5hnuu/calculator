package com.vi5hnu.calculator.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.MinTouchTarget
import com.vi5hnu.calculator.core.designsystem.SectionLabel
import com.vi5hnu.calculator.core.designsystem.SmallChip
import com.vi5hnu.calculator.core.designsystem.SpaceGrotesk
import com.vi5hnu.calculator.core.designsystem.SurfaceCard
import com.vi5hnu.calculator.core.designsystem.WarningNote
import com.vi5hnu.calculator.core.designsystem.fieldTextStyle

@Composable
fun ConverterScreen(
    state: ConverterUiState,
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Unit Converter")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            state.categories.forEach { name ->
                SmallChip(
                    label = name,
                    selected = name == state.category,
                    onClick = { viewModel.onCategorySelected(name) },
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }

        state.note?.let { WarningNote(it) }

        // FROM — the editable side.
        SurfaceCard(cornerRadius = 20.dp) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                UnitHeader(
                    label = "FROM",
                    unit = state.from,
                    units = state.units,
                    onSelect = viewModel::onFromSelected,
                )
                BasicTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    singleLine = true,
                    textStyle = fieldTextStyle(32.sp),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(MinTouchTarget)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable(role = Role.Button) { viewModel.onSwap() }
                    .semantics { contentDescription = "Swap ${state.from} and ${state.to}" },
                contentAlignment = Alignment.Center,
            ) {
                Text("⇅", color = colors.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // TO — computed.
        SurfaceCard(cornerRadius = 20.dp) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                UnitHeader(
                    label = "TO",
                    unit = state.to,
                    units = state.units,
                    onSelect = viewModel::onToSelected,
                )
                Text(
                    text = state.result,
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Caption plus the unit dropdown. */
@Composable
private fun UnitHeader(
    label: String,
    unit: String,
    units: List<String>,
    onSelect: (String) -> Unit,
) {
    val colors = MathProTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            fontFamily = SpaceGrotesk,
        )

        Box {
            SmallChip(label = "$unit ▾", selected = false, onClick = { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                units.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (option == unit) colors.accent else colors.textPrimary,
                                fontFamily = SpaceGrotesk,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
