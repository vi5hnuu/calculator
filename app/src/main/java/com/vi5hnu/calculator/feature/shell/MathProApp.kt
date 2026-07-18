package com.vi5hnu.calculator.feature.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vi5hnu.calculator.core.designsystem.Accent
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.ModeChip
import com.vi5hnu.calculator.core.designsystem.SpaceGrotesk
import com.vi5hnu.calculator.domain.model.CalcMode
import com.vi5hnu.calculator.domain.model.ThemeMode
import com.vi5hnu.calculator.feature.ads.AdBanner
import com.vi5hnu.calculator.feature.calc.CalcKeypad
import com.vi5hnu.calculator.feature.calc.CalcViewModel
import com.vi5hnu.calculator.feature.calc.CalcWorkArea
import com.vi5hnu.calculator.feature.calc.FunctionTray
import com.vi5hnu.calculator.feature.converter.ConverterScreen
import com.vi5hnu.calculator.feature.converter.ConverterViewModel
import com.vi5hnu.calculator.feature.date.DateScreen
import com.vi5hnu.calculator.feature.date.DateViewModel
import com.vi5hnu.calculator.feature.finance.FinanceScreen
import com.vi5hnu.calculator.feature.finance.FinanceViewModel
import com.vi5hnu.calculator.feature.graph.GraphScreen
import com.vi5hnu.calculator.feature.graph.GraphViewModel
import com.vi5hnu.calculator.feature.history.HistoryPanel
import com.vi5hnu.calculator.feature.history.HistoryViewModel
import com.vi5hnu.calculator.feature.percent.PercentScreen
import com.vi5hnu.calculator.feature.percent.PercentViewModel
import com.vi5hnu.calculator.feature.programmer.ProgrammerKeypad
import com.vi5hnu.calculator.feature.programmer.ProgrammerViewModel
import com.vi5hnu.calculator.feature.programmer.ProgrammerWorkArea
import com.vi5hnu.calculator.feature.worksheet.WorksheetScreen
import com.vi5hnu.calculator.feature.worksheet.WorksheetViewModel

/**
 * The app shell: status/top bar, mode dock, the active mode, and the history drawer.
 *
 * The ViewModels are resolved here rather than inside each mode, so switching modes does not
 * discard their state and the history drawer can hand an expression back to the calculator.
 */
@Composable
fun MathProApp(
    mode: CalcMode,
    themeMode: ThemeMode,
    accent: Accent,
    shellViewModel: ShellViewModel,
    adsAllowed: Boolean,
) {
    val colors = MathProTheme.colors
    val calcViewModel: CalcViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val historyOpen by shellViewModel.historyOpen.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush),
    ) {
        // safeDrawing, not systemBars: enableEdgeToEdge() stops `adjustResize` from resizing
        // the window, so without the IME inset the keyboard simply covers the field being
        // typed into. safeDrawing folds in system bars, the IME and any display cutout.
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(
                mode = mode,
                themeMode = themeMode,
                accent = accent,
                onToggleTheme = {
                    // Cycles system -> light -> dark, preserving the old app's toggle while
                    // adding an explicit "follow the system" position.
                    shellViewModel.setThemeMode(
                        when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                        },
                    )
                },
                onAccentSelected = shellViewModel::setAccent,
                onOpenHistory = { shellViewModel.setHistoryOpen(true) },
            )

            ModeDock(selected = mode, onSelect = shellViewModel::setMode)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ModeContent(
                    mode = mode,
                    calcViewModel = calcViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ModeKeypad(
                mode = mode,
                calcViewModel = calcViewModel,
                onToggleAngle = shellViewModel::toggleAngleUnit,
            )

            // Anchored at the very bottom, below every control, once consent permits ads.
            // Composed once and left alone — gating it on transient state (like whether the
            // keyboard is open) would destroy and reload the ad on every toggle.
            if (adsAllowed) {
                AdBanner()
            }
        }

        HistoryPanel(
            open = historyOpen,
            state = historyViewModel.state.collectAsStateWithLifecycle().value,
            onQueryChange = historyViewModel::onQueryChange,
            onReuse = { expression ->
                // Reusing an entry drops you back on the calculator with it loaded.
                calcViewModel.setExpression(expression)
                shellViewModel.setMode(CalcMode.SCIENTIFIC)
                shellViewModel.setHistoryOpen(false)
            },
            onTogglePin = historyViewModel::togglePin,
            onDelete = historyViewModel::delete,
            onClear = historyViewModel::clear,
            onDismiss = { shellViewModel.setHistoryOpen(false) },
        )
    }
}

/** Dispatches to the active mode's work area. */
@Composable
private fun ModeContent(
    mode: CalcMode,
    calcViewModel: CalcViewModel,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        CalcMode.STANDARD, CalcMode.SCIENTIFIC -> CalcWorkArea(
            state = calcViewModel.state.collectAsStateWithLifecycle().value,
            onReuse = calcViewModel::setExpression,
            modifier = modifier,
        )

        CalcMode.PROGRAMMER -> {
            val viewModel: ProgrammerViewModel = hiltViewModel()
            ProgrammerWorkArea(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                onBaseSelected = viewModel::onBaseSelected,
                modifier = modifier,
            )
        }

        CalcMode.CONVERT -> {
            val viewModel: ConverterViewModel = hiltViewModel()
            ConverterScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = modifier,
            )
        }

        CalcMode.GRAPH -> {
            val viewModel: GraphViewModel = hiltViewModel()
            GraphScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = modifier,
            )
        }

        CalcMode.FINANCE -> {
            val viewModel: FinanceViewModel = hiltViewModel()
            FinanceScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = modifier,
            )
        }

        CalcMode.DATE -> {
            val viewModel: DateViewModel = hiltViewModel()
            DateScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = modifier,
            )
        }

        CalcMode.PERCENT -> {
            val viewModel: PercentViewModel = hiltViewModel()
            PercentScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = modifier,
            )
        }

        CalcMode.WORKSHEET -> {
            val viewModel: WorksheetViewModel = hiltViewModel()
            WorksheetScreen(
                text = viewModel.text.collectAsStateWithLifecycle().value,
                lines = viewModel.lines.collectAsStateWithLifecycle().value,
                onTextChange = viewModel::onTextChange,
                modifier = modifier,
            )
        }
    }
}

/** Only the calculator and programmer modes carry a fixed keypad. */
@Composable
private fun ModeKeypad(
    mode: CalcMode,
    calcViewModel: CalcViewModel,
    onToggleAngle: () -> Unit,
) {
    when (mode) {
        CalcMode.STANDARD, CalcMode.SCIENTIFIC -> {
            val state by calcViewModel.state.collectAsStateWithLifecycle()
            if (mode == CalcMode.SCIENTIFIC) {
                FunctionTray(
                    second = state.second,
                    angleLabel = state.angleUnit.label,
                    onKey = calcViewModel::onKey,
                    onToggleAngle = onToggleAngle,
                )
            }
            CalcKeypad(onKey = calcViewModel::onKey, onMemory = calcViewModel::onMemoryKey)
        }

        CalcMode.PROGRAMMER -> {
            val viewModel: ProgrammerViewModel = hiltViewModel()
            ProgrammerKeypad(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
            )
        }

        else -> Unit
    }
}

@Composable
private fun TopBar(
    mode: CalcMode,
    themeMode: ThemeMode,
    accent: Accent,
    onToggleTheme: () -> Unit,
    onAccentSelected: (Accent) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val colors = MathProTheme.colors
    var accentMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accentBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text("∑", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column {
                Text(
                    text = "MathPro",
                    color = colors.textPrimary,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = mode.label.uppercase(),
                    color = colors.textMuted,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // One button, cycling system -> light -> dark. The icon reflects the *current* mode
            // so the control reads as a single toggle rather than three separate options.
            IconAction(
                icon = themeMode.icon,
                contentDescription = "Theme: ${themeMode.spokenName}. Tap to change.",
                onClick = onToggleTheme,
            )

            Box {
                IconAction(
                    icon = Icons.Rounded.Palette,
                    contentDescription = "Accent colour: ${accent.label}",
                    onClick = { accentMenuOpen = true },
                    tint = colors.accent,
                )
                DropdownMenu(
                    expanded = accentMenuOpen,
                    onDismissRequest = { accentMenuOpen = false },
                ) {
                    Accent.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(option.color),
                                    )
                                    Text(
                                        text = option.label,
                                        color = if (option == accent) {
                                            colors.accent
                                        } else {
                                            colors.textPrimary
                                        },
                                        fontFamily = SpaceGrotesk,
                                    )
                                }
                            },
                            onClick = {
                                onAccentSelected(option)
                                accentMenuOpen = false
                            },
                        )
                    }
                }
            }

            IconAction(
                icon = Icons.Rounded.History,
                contentDescription = "History",
                onClick = onOpenHistory,
            )
        }
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            // 48dp — Android's minimum touch target. The mock's 34dp read as fiddly.
            .size(48.dp)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // described on the box above, avoids a double read
            tint = tint ?: colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

private val ThemeMode.spokenName: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "follow system"
        ThemeMode.LIGHT -> "light"
        ThemeMode.DARK -> "dark"
    }

@Composable
private fun ModeDock(selected: CalcMode, onSelect: (CalcMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        CalcMode.entries.forEach { mode ->
            ModeChip(
                label = mode.label,
                selected = mode == selected,
                onClick = { onSelect(mode) },
            )
        }
    }
}
