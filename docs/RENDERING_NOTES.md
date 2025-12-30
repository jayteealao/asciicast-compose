# Rendering Notes

Terminal rendering implementation using Jetpack Compose Canvas.

## Strategy

### Text Runs (Not Per-Cell)

**Bad** (per-cell drawing):
```kotlin
for (row in 0 until rows) {
    for (col in 0 until cols) {
        val cell = buffer[row][col]
        drawText(cell.char, x = col * cellWidth, y = row * cellHeight)
    }
}
```

**Good** (text runs):
```kotlin
for (row in 0 until rows) {
    for (run in lines[row].runs) {
        drawText(run.text, x = run.colStart * cellWidth, y = row * cellHeight)
    }
}
```

**Why**:
- Reduces draw calls from `rows * cols` to `rows * runsPerLine`
- Typical line has 1-5 runs, not 80 cells
- Compose batches text rendering better with longer strings

### Cell Measurement

Use `TextMeasurer` to compute cell dimensions:

```kotlin
val textMeasurer = rememberTextMeasurer()

val cellDimensions = remember(fontSize, fontFamily) {
    val result = textMeasurer.measure(
        text = "M", // Monospace reference character
        style = TextStyle(fontSize = fontSize.sp, fontFamily = fontFamily)
    )

    CellDimensions(
        width = result.size.width.toFloat(),
        height = result.size.height.toFloat()
    )
}
```

**Notes**:
- Measure once per font change
- Use "M" as it's typically widest in monospace fonts
- Cache result in `remember`

### Scaling

Support multiple scale modes:

| Mode | Behavior |
|------|----------|
| **FitWidth** | Scale to canvas width, preserve aspect ratio |
| **FitHeight** | Scale to canvas height, preserve aspect ratio |
| **FitBoth** | Scale to fit both (maintains aspect, may have margins) |
| **None** | 1:1 pixel mapping (may overflow or underflow) |

Formula:

```kotlin
val scaleX = canvasWidth / (cols * cellWidth)
val scaleY = canvasHeight / (rows * cellHeight)

val scale = when (scaleMode) {
    FitWidth -> scaleX
    FitHeight -> scaleY
    FitBoth -> minOf(scaleX, scaleY)
    None -> 1f
}
```

### Drawing Order

1. **Background**: Draw solid color for canvas
2. **Cell backgrounds**: Draw rectangles for non-default backgrounds
3. **Text**: Draw text runs with styles
4. **Cursor**: Draw cursor indicator (underline, block, or bar)

### Performance Optimizations

#### 1. Skip Default Backgrounds

```kotlin
if (bgColor != theme.background) {
    drawRect(bgColor, topLeft, size)
}
```

Only draw rectangles for cells with non-default backgrounds.

#### 2. Batch Text Styles

Group consecutive cells with same style into single `drawText` call:

```kotlin
data class TextRun(
    val colStart: Int,
    val text: String,
    val style: CellStyle
)
```

#### 3. Recomposition Scope

Use `StateFlow<TerminalFrame>` to limit recomposition:

```kotlin
val frame by player.frame.collectAsState()

TerminalCanvas(frame = frame, ...)
```

Only recomposes when frame changes (not on every player state change).

#### 4. Differential Updates (Future)

```kotlin
val diff = vt.pollDiff()

if (diff != null && diff != TerminalDiff.FULL) {
    // Only redraw dirty lines
    diff.dirtyLines.forEach { row ->
        redrawLine(row)
    }
} else {
    // Full redraw
    redrawAll()
}
```

Not yet implemented; current version always does full snapshots.

## Cursor Rendering

### Underline Style

```kotlin
if (cursor.visible) {
    drawRect(
        color = theme.foreground.copy(alpha = 0.7f),
        topLeft = Offset(cursor.col * cellWidth, cursor.row * cellHeight + cellHeight * 0.8f),
        size = Size(cellWidth, cellHeight * 0.2f)
    )
}
```

### Block Style (Alternative)

```kotlin
drawRect(
    color = theme.foreground.copy(alpha = 0.3f),
    topLeft = Offset(cursor.col * cellWidth, cursor.row * cellHeight),
    size = Size(cellWidth, cellHeight)
)
```

### Bar Style (Alternative)

```kotlin
drawRect(
    color = theme.foreground,
    topLeft = Offset(cursor.col * cellWidth, cursor.row * cellHeight),
    size = Size(cellWidth * 0.1f, cellHeight)
)
```

## Text Attributes

Map `CellStyle` to Compose `TextStyle`:

```kotlin
TextStyle(
    fontSize = fontSize.sp,
    fontFamily = fontFamily,
    color = fgColor,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    textDecoration = when {
        underline && strikethrough -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )
        underline -> TextDecoration.Underline
        strikethrough -> TextDecoration.LineThrough
        else -> null
    }
)
```

**Note**: Compose doesn't support `faint` or `blink` natively. Faint can be approximated with alpha:

```kotlin
color = fgColor.copy(alpha = if (faint) 0.5f else 1f)
```

## Color Resolution

### Theme Palette

```kotlin
fun Theme.resolve(color: Color): Color.Rgb {
    return when (color) {
        is Color.Default -> foreground
        is Color.Rgb -> color
        is Color.Indexed -> when {
            color.index < 8 -> palette8?.get(color.index) ?: Theme.STANDARD_8[color.index]
            color.index < 16 -> palette16?.get(color.index) ?: Theme.STANDARD_16[color.index]
            color.index < 256 -> xterm256Color(color.index)
            else -> foreground
        }
    }
}
```

### Reverse Video

Swap foreground and background when `reverse` is set:

```kotlin
val fgColor = if (style.reverse) {
    theme.resolve(style.background)
} else {
    theme.resolve(style.foreground)
}

val bgColor = if (style.reverse) {
    theme.resolve(style.foreground)
} else {
    theme.resolve(style.background)
}
```

## Limitations

### What Works

- ✅ 8/16/256 color palettes
- ✅ True color RGB
- ✅ Bold, italic, underline, strikethrough
- ✅ Reverse video
- ✅ Cursor rendering

### What Doesn't Work (Yet)

- ❌ Faint (approximated with alpha)
- ❌ Blink (Compose doesn't support)
- ❌ Double underline (use single)
- ❌ Overline (not in Compose)
- ❌ Per-cell background images

### Workarounds

**Faint**: Use alpha transparency:
```kotlin
if (style.faint) {
    color = color.copy(alpha = 0.5f)
}
```

**Blink**: Could implement with LaunchedEffect timer, but not recommended (accessibility).

## Testing

### Manual Testing

```kotlin
val testFrame = TerminalFrame(
    cols = 80,
    rows = 24,
    lines = listOf(
        TerminalLine(
            runs = listOf(
                TextRun(0, "Hello ", CellStyle(foreground = Color.Indexed(1))),
                TextRun(6, "World", CellStyle(bold = true))
            )
        )
    ),
    cursor = Cursor(0, 11, true)
)

TerminalCanvas(frame = testFrame)
```

### Screenshot Tests (Optional)

Use Compose testing to capture and compare screenshots:

```kotlin
@Test
fun testRenderingSnapshot() {
    composeTestRule.setContent {
        TerminalCanvas(frame = testFrame)
    }

    composeTestRule.onRoot().captureToImage()
        .assertAgainstGolden("terminal_frame")
}
```

## Future Enhancements

1. **Hardware acceleration**: Use Skia/OpenGL for large grids
2. **Diff rendering**: Only redraw changed lines
3. **Caching**: Cache text layouts for unchanged runs
4. **Sixel graphics**: Inline image support
5. **Ligatures**: Enable font ligatures for better code rendering
