package uk.adedamola.asciicast.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import uk.adedamola.asciicast.vt.*

/**
 * Render a terminal frame onto a Compose Canvas.
 *
 * @param frame The terminal frame to render
 * @param modifier Modifier for the canvas
 * @param fontFamily Font to use (default: monospace)
 * @param fontSize Font size in SP (default: 14)
 * @param scaleMode How to scale the terminal to fit the canvas
 * @param themeOverride Optional theme override
 */
@Composable
fun TerminalCanvas(
    frame: TerminalFrame,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
    fontSize: Int = 14,
    scaleMode: ScaleMode = ScaleMode.FitBoth,
    themeOverride: Theme? = null
) {
    val theme = themeOverride ?: frame.theme

    // Measure text to compute cell dimensions
    val textMeasurer = rememberTextMeasurer()

    val cellDimensions = remember(fontSize, fontFamily) {
        measureCellDimensions(textMeasurer, fontSize, fontFamily)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = computeScale(
            canvasSize = size,
            gridCols = frame.cols,
            gridRows = frame.rows,
            cellWidth = cellDimensions.width,
            cellHeight = cellDimensions.height,
            scaleMode = scaleMode
        )

        // Apply scale and draw terminal
        drawTerminal(
            frame = frame,
            theme = theme,
            cellWidth = cellDimensions.width * scale,
            cellHeight = cellDimensions.height * scale,
            textMeasurer = textMeasurer,
            fontSize = fontSize,
            fontFamily = fontFamily,
            scale = scale
        )
    }
}

/**
 * Terminal scaling modes.
 */
enum class ScaleMode {
    /** Fit to canvas width */
    FitWidth,

    /** Fit to canvas height */
    FitHeight,

    /** Fit to both dimensions (may distort aspect ratio) */
    FitBoth,

    /** No scaling (1:1 pixels) */
    None
}

/**
 * Cell dimensions in pixels.
 */
private data class CellDimensions(
    val width: Float,
    val height: Float
)

/**
 * Measure cell dimensions by rendering a sample character.
 */
private fun measureCellDimensions(
    textMeasurer: TextMeasurer,
    fontSize: Int,
    fontFamily: androidx.compose.ui.text.font.FontFamily
): CellDimensions {
    val textLayoutResult = textMeasurer.measure(
        text = "M", // Use 'M' as typical monospace character
        style = TextStyle(
            fontSize = fontSize.sp,
            fontFamily = fontFamily
        )
    )

    return CellDimensions(
        width = textLayoutResult.size.width.toFloat(),
        height = textLayoutResult.size.height.toFloat()
    )
}

/**
 * Compute scaling factor based on canvas size and grid dimensions.
 */
private fun computeScale(
    canvasSize: Size,
    gridCols: Int,
    gridRows: Int,
    cellWidth: Float,
    cellHeight: Float,
    scaleMode: ScaleMode
): Float {
    return when (scaleMode) {
        ScaleMode.None -> 1f

        ScaleMode.FitWidth -> {
            canvasSize.width / (gridCols * cellWidth)
        }

        ScaleMode.FitHeight -> {
            canvasSize.height / (gridRows * cellHeight)
        }

        ScaleMode.FitBoth -> {
            val scaleX = canvasSize.width / (gridCols * cellWidth)
            val scaleY = canvasSize.height / (gridRows * cellHeight)
            minOf(scaleX, scaleY)
        }
    }
}

/**
 * Draw the terminal frame.
 */
private fun DrawScope.drawTerminal(
    frame: TerminalFrame,
    theme: Theme,
    cellWidth: Float,
    cellHeight: Float,
    textMeasurer: TextMeasurer,
    fontSize: Int,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    scale: Float
) {
    // Draw background
    drawRect(
        color = theme.background.toComposeColor(),
        size = size
    )

    // Draw each line
    frame.lines.forEachIndexed { rowIndex, line ->
        val y = rowIndex * cellHeight

        // Draw cell backgrounds for runs
        line.runs.forEach { run ->
            val x = run.colStart * cellWidth
            val bgColor = if (run.style.reverse) {
                theme.resolve(run.style.foreground).toComposeColor()
            } else {
                theme.resolve(run.style.background).toComposeColor()
            }

            if (bgColor != theme.background.toComposeColor()) {
                drawRect(
                    color = bgColor,
                    topLeft = Offset(x, y),
                    size = Size(run.length * cellWidth, cellHeight)
                )
            }
        }

        // Draw text runs
        line.runs.forEach { run ->
            val x = run.colStart * cellWidth
            val fgColor = if (run.style.reverse) {
                theme.resolve(run.style.background)
            } else {
                theme.resolve(run.style.foreground)
            }

            drawText(
                textMeasurer = textMeasurer,
                text = run.text,
                topLeft = Offset(x, y),
                style = TextStyle(
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    color = fgColor.toComposeColor(),
                    fontWeight = if (run.style.bold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontStyle = if (run.style.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = when {
                        run.style.underline && run.style.strikethrough -> TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                        )
                        run.style.underline -> TextDecoration.Underline
                        run.style.strikethrough -> TextDecoration.LineThrough
                        else -> null
                    }
                )
            )
        }
    }

    // Draw cursor
    if (frame.cursor.visible && frame.cursor.row < frame.rows && frame.cursor.col < frame.cols) {
        val cursorX = frame.cursor.col * cellWidth
        val cursorY = frame.cursor.row * cellHeight

        drawRect(
            color = theme.foreground.toComposeColor().copy(alpha = 0.7f),
            topLeft = Offset(cursorX, cursorY),
            size = Size(cellWidth, cellHeight * 0.2f) // Underline cursor
        )
    }
}

/**
 * Convert vt Color to Compose Color.
 */
private fun uk.adedamola.asciicast.vt.Color.Rgb.toComposeColor(): ComposeColor {
    return ComposeColor(r, g, b)
}
