package com.vi5hnu.calculator.feature.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.KeyButton
import com.vi5hnu.calculator.core.designsystem.KeyTone
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

data class GraphUiState(
    val expression: String = "sin(x)",
    val span: Double = 10.0,
) {
    val rangeLabel: String get() = "${format(-span)}, ${format(span)}"

    private fun format(v: Double) = if (v == floor(v)) v.toInt().toString() else "%.2f".format(v)
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val engine: ExpressionEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(GraphUiState())
    val state: StateFlow<GraphUiState> = _state.asStateFlow()

    fun onExpressionChange(value: String) = _state.update { it.copy(expression = value) }

    fun onZoom(zoomIn: Boolean) = _state.update {
        it.copy(span = (if (zoomIn) it.span / 1.6 else it.span * 1.6).coerceIn(1.0, 200.0))
    }

    /**
     * Samples y across the visible x range.
     *
     * Always evaluated in radians regardless of the user's angle setting — a sine wave plotted
     * in degrees would be a nearly flat line over x ∈ [-10, 10] and read as a bug. This matches
     * the design, which forces radians when graphing.
     */
    fun sample(points: Int): List<Double> {
        val span = _state.value.span
        val expression = _state.value.expression
        return (0..points).map { i ->
            val x = -span + 2 * span * i / points
            (engine.evaluate(expression, AngleUnit.RAD, mapOf("x" to x)) as? EvalOutcome.Success)
                ?.value ?: Double.NaN
        }
    }
}

@Composable
fun GraphScreen(
    state: GraphUiState,
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // y = <expression>
        val fieldShape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(colors.surfaceStrong)
                .border(BorderStroke(1.dp, colors.border), fieldShape)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "y =",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            BasicTextField(
                value = state.expression,
                onValueChange = viewModel::onExpressionChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Plot(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyButton(
                label = "−",
                onClick = { viewModel.onZoom(zoomIn = false) },
                modifier = Modifier.weight(1f),
                height = 48.dp,
                fontSize = 18.sp,
                cornerRadius = 12.dp,
            )
            Text(
                text = "x ∈ [${state.rangeLabel}]",
                modifier = Modifier.weight(2f),
                color = colors.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            KeyButton(
                label = "+",
                onClick = { viewModel.onZoom(zoomIn = true) },
                modifier = Modifier.weight(1f),
                height = 48.dp,
                fontSize = 18.sp,
                cornerRadius = 12.dp,
            )
        }
    }
}

@Composable
private fun Plot(
    state: GraphUiState,
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (colors.isDark) colors.backgroundBottom else colors.keyTop)
            .border(BorderStroke(1.dp, colors.border), shape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // One sample per horizontal pixel — more is wasted, fewer shows as facets.
            val samples = viewModel.sample(width.toInt().coerceAtLeast(2))
            val span = state.span

            val finite = samples.filter { it.isFinite() }
            var yMax = finite.maxOrNull() ?: span
            var yMin = finite.minOrNull() ?: -span
            // A constant function (or no finite samples) has no natural vertical extent.
            if (!yMax.isFinite() || !yMin.isFinite() || yMax - yMin < 1e-6) {
                yMax = span
                yMin = -span
            }
            val pad = ((yMax - yMin) * 0.1).takeIf { it > 0 } ?: 1.0
            yMax += pad
            yMin -= pad

            fun px(x: Double) = ((x + span) / (2 * span) * width).toFloat()
            fun py(y: Double) = (height - (y - yMin) / (yMax - yMin) * height).toFloat()

            // Grid: one line per integer x, ten evenly spaced rows.
            val gridColor = colors.textPrimary.copy(alpha = 0.06f)
            var gx = ceil(-span)
            while (gx <= span) {
                drawLine(gridColor, Offset(px(gx), 0f), Offset(px(gx), height), 1f)
                gx += 1.0
            }
            repeat(11) { i ->
                val y = yMin + (yMax - yMin) * i / 10.0
                drawLine(gridColor, Offset(0f, py(y)), Offset(width, py(y)), 1f)
            }

            // Axes.
            val axisColor = colors.textPrimary.copy(alpha = 0.25f)
            if (yMin <= 0.0 && yMax >= 0.0) {
                drawLine(axisColor, Offset(0f, py(0.0)), Offset(width, py(0.0)), 2f)
            }
            drawLine(axisColor, Offset(px(0.0), 0f), Offset(px(0.0), height), 2f)

            // Curve. Breaks the path at discontinuities (tan, 1/x) rather than drawing the
            // vertical line an unbroken stroke would produce across an asymptote.
            val path = Path()
            var started = false
            samples.forEachIndexed { i, y ->
                if (!y.isFinite() || abs(py(y)) > height * 10) {
                    started = false
                    return@forEachIndexed
                }
                val x = -span + 2 * span * i / (samples.size - 1)
                val point = Offset(px(x), py(y))
                if (started) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                started = true
            }
            drawPath(
                path = path,
                color = colors.accent,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
