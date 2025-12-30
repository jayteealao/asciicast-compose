# Adding a VT Backend

This guide walks through implementing a new `VirtualTerminal` backend.

## Step 1: Create Module

```bash
mkdir -p vt-mybackend/src/main/kotlin/uk/adedamola/asciicast/vt/mybackend
```

Create `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") // or id("com.android.library") + kotlin("android")
}

dependencies {
    api(project(":vt-api"))

    // Your backend dependency
    // implementation("com.example:terminal-lib:x.y.z")

    testImplementation(kotlin("test"))
}
```

Add to `settings.gradle.kts`:

```kotlin
include(":vt-mybackend")
```

## Step 2: Implement VirtualTerminal

Create `MyBackendVirtualTerminal.kt`:

```kotlin
package uk.adedamola.asciicast.vt.mybackend

import uk.adedamola.asciicast.vt.*

class MyBackendVirtualTerminal(
    initialCols: Int = 80,
    initialRows: Int = 24
) : VirtualTerminal {

    // TODO: Create backend instance
    // private val terminal = MyTerminal(initialCols, initialRows)

    override var cols: Int = initialCols
        private set

    override var rows: Int = initialRows
        private set

    override val capabilities: TermCapabilities = TermCapabilities(
        trueColor = true, // Set based on backend capabilities
        faint = true,
        italic = true,
        strikethrough = true,
        altScreen = true,
        mouse = false,
        bracketedPaste = false,
        differentialUpdates = true // If backend provides dirty tracking
    )

    override fun reset(cols: Int, rows: Int, theme: Theme?, initData: String?) {
        // TODO: Reset backend terminal
        this.cols = cols
        this.rows = rows
        // Apply theme if backend supports it
        // Feed initData if provided
    }

    override fun resize(cols: Int, rows: Int) {
        // TODO: Call backend resize
        this.cols = cols
        this.rows = rows
    }

    override fun feedUtf8(text: String) {
        feed(text.toByteArray(Charsets.UTF_8))
    }

    override fun feed(bytes: ByteArray) {
        // TODO: Feed bytes to backend parser
    }

    override fun snapshot(): TerminalFrame {
        // TODO: Read backend buffer and convert to TerminalFrame
        return convertToTerminalFrame()
    }

    override fun pollDiff(): TerminalDiff? {
        // TODO: If backend tracks dirty regions, return diff
        // Otherwise return null (will use full snapshots)
        return null
    }

    override fun close() {
        // TODO: Clean up backend resources
    }

    private fun convertToTerminalFrame(): TerminalFrame {
        // TODO: Implement conversion logic
        // 1. Read backend buffer (lines)
        // 2. For each line, group consecutive cells with same style into TextRuns
        // 3. Extract cursor position and visibility
        // 4. Return TerminalFrame

        val lines = (0 until rows).map { row ->
            convertLine(row)
        }

        return TerminalFrame(
            cols = cols,
            rows = rows,
            lines = lines,
            cursor = Cursor(/* row */, /* col */, /* visible */),
            theme = Theme.DEFAULT
        )
    }

    private fun convertLine(row: Int): TerminalLine {
        // TODO: Read backend line at row
        // Group consecutive cells with same style

        val runs = mutableListOf<TextRun>()

        // Pseudocode:
        // var currentRun: TextRun? = null
        // for (col in 0 until cols) {
        //     val cell = backend.getCell(row, col)
        //     val style = convertStyle(cell.style)
        //     val char = cell.char
        //
        //     if (currentRun == null || currentRun.style != style) {
        //         // Start new run
        //         currentRun?.let { runs.add(it) }
        //         currentRun = TextRun(col, char.toString(), style)
        //     } else {
        //         // Continue run
        //         currentRun = currentRun.copy(text = currentRun.text + char)
        //     }
        // }
        // currentRun?.let { runs.add(it) }

        return TerminalLine(runs)
    }

    private fun convertStyle(/* backend style */): CellStyle {
        // TODO: Map backend style attributes to CellStyle
        return CellStyle(
            foreground = /* ... */,
            background = /* ... */,
            bold = /* ... */,
            italic = /* ... */,
            underline = /* ... */,
            // ...
        )
    }
}
```

## Step 3: Map Backend Styles

Mapping depends on your backend's color/style representation:

### Example: 8/16 Color Palette

```kotlin
fun convertColor(backendColor: Int): Color {
    return when {
        backendColor < 0 -> Color.Default
        backendColor < 16 -> Color.Indexed(backendColor)
        else -> Color.Rgb(/* extract RGB from backendColor */)
    }
}
```

### Example: True Color

```kotlin
fun convertColor(r: Int, g: Int, b: Int): Color {
    return Color.Rgb(r, g, b)
}
```

## Step 4: Handle Dirty Tracking (Optional)

If your backend provides dirty line tracking:

```kotlin
private val dirtyLines = mutableSetOf<Int>()

override fun feed(bytes: ByteArray) {
    backend.feed(bytes)

    // Collect dirty lines from backend
    backend.getDirtyLines().forEach { dirtyLines.add(it) }
}

override fun pollDiff(): TerminalDiff {
    if (dirtyLines.isEmpty()) {
        return TerminalDiff.NONE
    }

    val diff = TerminalDiff(
        dirtyLines = dirtyLines.toSet(),
        cursorChanged = backend.cursorChanged(),
        resized = false
    )

    dirtyLines.clear()
    return diff
}
```

## Step 5: Write Tests

Create `MyBackendVirtualTerminalTest.kt`:

```kotlin
class MyBackendVirtualTerminalTest {
    private lateinit var vt: MyBackendVirtualTerminal

    @BeforeTest
    fun setup() {
        vt = MyBackendVirtualTerminal()
    }

    @AfterTest
    fun teardown() {
        vt.close()
    }

    @Test
    fun `reset initializes terminal`() {
        vt.reset(80, 24)
        assertEquals(80, vt.cols)
        assertEquals(24, vt.rows)
    }

    @Test
    fun `feed processes ANSI codes`() {
        vt.feedUtf8("\u001b[31mRed Text")
        val frame = vt.snapshot()

        // Verify red foreground color
        val firstRun = frame.lines[0].runs.firstOrNull()
        assertNotNull(firstRun)
        // Assert color is red
    }

    @Test
    fun `cursor position tracks correctly`() {
        vt.feedUtf8("Hello")
        val frame = vt.snapshot()

        assertEquals(0, frame.cursor.row)
        assertEquals(5, frame.cursor.col) // After "Hello"
    }
}
```

## Step 6: Document Backend

Create `vt-mybackend/README.md`:

```markdown
# My Backend VirtualTerminal

Implementation using [Backend Name].

## Features

- VT100/xterm compatibility
- True color support
- Dirty line tracking

## Dependencies

- my-backend-lib: x.y.z

## Platform Support

- [x] Android
- [ ] JVM Desktop
- [ ] iOS

## Notes

- Special considerations for this backend
- Performance characteristics
- Known limitations
```

## Step 7: Integration Example

Show how to use in player-core:

```kotlin
val vt = MyBackendVirtualTerminal()
val player = AsciinemaPlayer(vt, coroutineScope)

player.load(RecordingSource(inputStream))
player.play()
```

## Common Pitfalls

1. **Line-by-line vs Cell-by-cell**:
   - Group cells into TextRuns for efficiency
   - Don't create one run per cell

2. **Cursor Handling**:
   - Some backends have hidden cursor state
   - Check visibility flag before rendering

3. **Default Colors**:
   - Map backend "default" to `Color.Default`
   - Renderer resolves via theme

4. **UTF-8 Handling**:
   - Some backends require byte input
   - Others work with String
   - Provide both `feed` and `feedUtf8`

5. **Thread Safety**:
   - VT should NOT be thread-safe (player serializes calls)
   - Document this clearly

6. **Resource Cleanup**:
   - Implement `close()` properly
   - Release backend resources

## Example: JediTerm Adapter

See `vt-jediterm/` for a TODO-scaffolded example of adapting JediTerm.

## Example: Termux Adapter

See `vt-termux/` for a TODO-scaffolded example with licensing notes.
