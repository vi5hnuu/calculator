package com.vi5hnu.calculator.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.SpaceGrotesk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The slide-over history drawer.
 *
 * Carries forward the two history behaviours the Flutter app had — persistence and
 * tap-to-reuse — and adds the design's search, pinning and per-row delete.
 */
@Composable
fun HistoryPanel(
    open: Boolean,
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onReuse: (String) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors

    // Clear wipes every entry, pinned ones included, with no undo. A single tap should not be
    // able to do that silently, so it routes through a confirmation.
    var confirmingClear by remember { mutableStateOf(false) }

    // Back closes the drawer rather than leaving the app, which is what an open overlay
    // should do on Android.
    BackHandler(enabled = open, onBack = onDismiss)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            // Scrim. `indication = null` keeps a dismissing tap from flashing a ripple.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val panelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight()
                    // A rounded left edge makes the panel read as a sheet sliding over the app
                    // rather than a hard wall bisecting the screen.
                    .clip(panelShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.backgroundTop, colors.backgroundMid),
                        ),
                    )
                    .border(BorderStroke(1.dp, colors.accent.copy(alpha = 0.15f)), panelShape)
                    // Keeps the search field above the keyboard.
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp),
            ) {
                DrawerHeader(onClear = { confirmingClear = true }, onDismiss = onDismiss)
                SearchField(value = state.query, onValueChange = onQueryChange)

                if (state.isEmpty) {
                    EmptyMessage("No calculations yet")
                } else if (state.rows.isEmpty()) {
                    EmptyMessage("Nothing matches \"${state.query}\"")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 4.dp,
                            bottom = 20.dp,
                        ),
                    ) {
                        items(state.rows, key = { it.id }) { row ->
                            HistoryRowItem(
                                row = row,
                                onReuse = { onReuse(row.expression) },
                                onTogglePin = { onTogglePin(row.id, row.pinned) },
                                onDelete = { onDelete(row.id) },
                            )
                        }
                    }
                }
            }
        }

        if (confirmingClear) {
            ConfirmClearDialog(
                onConfirm = {
                    onClear()
                    confirmingClear = false
                },
                onDismiss = { confirmingClear = false },
            )
        }
    }
}

@Composable
private fun ConfirmClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MathProTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear history?", color = colors.textPrimary, fontFamily = SpaceGrotesk) },
        text = {
            Text(
                "This deletes every entry, including pinned ones. It cannot be undone.",
                color = colors.textSecondary,
                fontFamily = SpaceGrotesk,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear", color = colors.danger, fontFamily = SpaceGrotesk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted, fontFamily = SpaceGrotesk)
            }
        },
        containerColor = colors.backgroundTop,
    )
}

@Composable
private fun DrawerHeader(onClear: () -> Unit, onDismiss: () -> Unit) {
    val colors = MathProTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "History",
            color = colors.textPrimary,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val clearShape = RoundedCornerShape(22.dp)
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(clearShape)
                    .background(colors.dangerSurface)
                    .border(BorderStroke(1.dp, colors.dangerBorder), clearShape)
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Clear",
                    color = colors.danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close history",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(colors.surfaceStrong)
            .border(BorderStroke(1.dp, colors.border), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontFamily = SpaceGrotesk,
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("Search history…", color = colors.textFaint, fontSize = 14.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MathProTheme.colors.textFaint, fontSize = 13.sp)
    }
}

@Composable
private fun HistoryRowItem(
    row: HistoryRow,
    onReuse: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            // Pinned rows carry a faint accent wash and border so they read as "kept" at a
            // glance, rather than relying on the star alone.
            .background(if (row.pinned) colors.accent.copy(alpha = 0.06f) else colors.surface)
            .border(
                BorderStroke(1.dp, if (row.pinned) colors.accent.copy(alpha = 0.25f) else colors.border),
                shape,
            )
            .clickable(role = Role.Button, onClick = onReuse)
            .semantics(mergeDescendants = true) {
                contentDescription = "${row.expression} equals ${row.result}. Tap to reuse."
            }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.expression,
                color = colors.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "= ${row.result}",
                color = colors.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatTimestamp(row.createdAt),
                color = colors.textFaint,
                fontSize = 10.sp,
                fontFamily = SpaceGrotesk,
            )
        }

        // Trailing controls. Material IconButton is 48dp, so both clear the touch minimum
        // without inflating the row.
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = if (row.pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (row.pinned) "Unpin" else "Pin",
                tint = if (row.pinned) colors.accent else colors.textFaint,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete",
                tint = colors.textFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM d · HH:mm")

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TIMESTAMP_FORMAT)
