package uk.adedamola.asciicast.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import uk.adedamola.asciicast.formats.AsciicastParser
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AsciinemaPlayerTest {

    private lateinit var fakeTerminal: FakeTerminal
    private lateinit var player: AsciinemaPlayer
    private lateinit var testScope: TestScope

    @BeforeTest
    fun setup() {
        testScope = TestScope()
        fakeTerminal = FakeTerminal()
        player = AsciinemaPlayer(fakeTerminal, testScope)
    }

    @AfterTest
    fun teardown() {
        player.close()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `load recording initializes terminal`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Hello"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)

        // Should be in Paused state after loading
        assertIs<PlayerState.Paused>(player.state.value)

        // Terminal should have been reset
        val resetOps = fakeTerminal.operations.filterIsInstance<FakeTerminal.Operation.Reset>()
        assertEquals(1, resetOps.size)
        assertEquals(80, resetOps[0].cols)
        assertEquals(24, resetOps[0].rows)
    }

    @Test
    fun `play feeds events to terminal`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Hello "]
            [0.001,"o","World"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.play()

        // Wait for playback to complete
        testScope.testScheduler.advanceUntilIdle()

        // Check that output was fed to terminal
        val fedText = fakeTerminal.getAllFedText()
        assertTrue(fedText.contains("Hello "))
        assertTrue(fedText.contains("World"))
    }

    @Test
    fun `pause stops playback`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Start"]
            [10.0,"o","End"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.play()

        // Pause immediately
        player.pause()

        // State should be Paused
        assertIs<PlayerState.Paused>(player.state.value)
    }

    @Test
    fun `stop resets to Idle state`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Test"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.stop()

        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `setSpeed adjusts playback rate`() = runTest {
        player.setSpeed(2.0f)

        // Should not throw
        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `setIdleTimeLimit configures compression`() = runTest {
        player.setIdleTimeLimit(1.0)

        // Should not throw
        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `markers are collected during playback`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"m","Chapter 1"]
            [1.0,"o","Content"]
            [2.0,"m","Chapter 2"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.play()

        testScope.testScheduler.advanceUntilIdle()

        // Check markers were collected
        val markers = player.markers.value
        assertEquals(2, markers.size)
        assertEquals("Chapter 1", markers[0].label)
        assertEquals("Chapter 2", markers[1].label)
    }

    @Test
    fun `resize event updates terminal size`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"r","100x30"]
            [1.0,"o","After resize"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.play()

        testScope.testScheduler.advanceUntilIdle()

        val resizeOps = fakeTerminal.operations.filterIsInstance<FakeTerminal.Operation.Resize>()
        assertEquals(1, resizeOps.size)
        assertEquals(100, resizeOps[0].cols)
        assertEquals(30, resizeOps[0].rows)
    }

    @Test
    fun `playback ends with Ended state`() = runTest {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Only event"]
        """.trimIndent()

        val source = RecordingSource(cast.byteInputStream())
        player.load(source)
        player.play()

        testScope.testScheduler.advanceUntilIdle()

        // Final state should be Ended
        assertEquals(PlayerState.Ended, player.state.value)
    }
}
