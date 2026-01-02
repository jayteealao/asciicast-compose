package uk.adedamola.asciicast.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uk.adedamola.asciicast.vt.*

/**
 * Asciinema player engine.
 *
 * Coordinates playback of terminal events through a VirtualTerminal backend.
 * Handles timing, speed control, idle time compression, and frame updates.
 *
 * Thread safety: All methods should be called from the same dispatcher/thread.
 */
class AsciinemaPlayer(
    private val virtualTerminal: VirtualTerminal,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _frame = MutableStateFlow(TerminalFrame.empty(80, 24))
    val frame: StateFlow<TerminalFrame> = _frame.asStateFlow()

    private val _markers = MutableStateFlow<List<Marker>>(emptyList())
    val markers: StateFlow<List<Marker>> = _markers.asStateFlow()

    private var playbackJob: Job? = null
    private var currentSource: PlaybackSource? = null

    // Playback controls
    private var playbackSpeed = 1.0f
    private var idleTimeLimitMicros: Long? = null
    private var elapsedTimeMicros = 0L

    /**
     * Load a playback source.
     */
    suspend fun load(source: PlaybackSource) {
        stop()

        _state.value = PlayerState.Loading
        currentSource = source

        try {
            val initEvent = source.init()

            // Apply idle time limit from metadata if present
            source.metadata.idleTimeLimit?.let {
                idleTimeLimitMicros = (it * 1_000_000).toLong()
            }

            // Reset terminal with initial state
            virtualTerminal.reset(
                cols = initEvent.cols,
                rows = initEvent.rows,
                theme = initEvent.theme,
                initData = initEvent.initData,
            )

            _frame.value = virtualTerminal.snapshot()
            _state.value = PlayerState.Paused(0)
        } catch (e: Exception) {
            _state.value = PlayerState.Error("Failed to load source", e)
        }
    }

    /**
     * Start or resume playback.
     */
    fun play() {
        val source = currentSource ?: return

        if (_state.value is PlayerState.Playing) return

        _state.value = PlayerState.Playing(playbackSpeed)

        playbackJob =
            scope.launch {
                try {
                    playEvents(source)
                    _state.value = PlayerState.Ended
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = PlayerState.Error("Playback error", e)
                }
            }
    }

    /**
     * Pause playback.
     */
    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = PlayerState.Paused(elapsedTimeMicros)
    }

    /**
     * Stop playback and unload source.
     */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        currentSource = null
        elapsedTimeMicros = 0L
        _markers.value = emptyList()
        _state.value = PlayerState.Idle
    }

    /**
     * Set playback speed multiplier.
     */
    fun setSpeed(speed: Float) {
        require(speed > 0) { "Speed must be positive" }
        playbackSpeed = speed

        if (_state.value is PlayerState.Playing) {
            _state.value = PlayerState.Playing(playbackSpeed)
        }
    }

    /**
     * Set idle time compression limit in seconds.
     */
    fun setIdleTimeLimit(seconds: Double?) {
        idleTimeLimitMicros = seconds?.let { (it * 1_000_000).toLong() }
    }

    /**
     * Seek to a specific time (TODO: requires event indexing).
     */
    fun seekTo(timeMicros: Long) {
        // TODO: Implement seeking
        // Requires building an event index or replaying from start
        throw UnsupportedOperationException("Seeking not yet implemented")
    }

    /**
     * Release resources.
     */
    fun close() {
        stop()
        virtualTerminal.close()
    }

    /**
     * Core playback loop.
     */
    private suspend fun playEvents(source: PlaybackSource) {
        println("[AsciinemaPlayer] playEvents: Starting playback loop")
        val markerList = mutableListOf<Marker>()
        var accumulatedDelayMicros = 0L

        source.events().collect { timedEvent ->
            val (event, deltaMicros) = timedEvent
            println("[AsciinemaPlayer] playEvents: Received event: $event, delta: $deltaMicros")

            // Apply idle time compression
            val effectiveDelta =
                idleTimeLimitMicros?.let { limit ->
                    minOf(deltaMicros, limit)
                } ?: deltaMicros

            // Apply speed adjustment and delay
            val adjustedDelayMicros = (effectiveDelta / playbackSpeed).toLong()
            accumulatedDelayMicros += effectiveDelta
            elapsedTimeMicros = accumulatedDelayMicros

            if (adjustedDelayMicros > 0) {
                delay(adjustedDelayMicros / 1000) // Convert to milliseconds
            }

            // Apply event to terminal
            when (event) {
                is TermEvent.Init -> {
                    virtualTerminal.reset(
                        cols = event.cols,
                        rows = event.rows,
                        theme = event.theme,
                        initData = event.initData,
                    )
                }

                is TermEvent.Output -> {
                    virtualTerminal.feedUtf8(event.data)
                }

                is TermEvent.Input -> {
                    // Ignore input events for read-only playback
                }

                is TermEvent.Resize -> {
                    virtualTerminal.resize(event.cols, event.rows)
                }

                is TermEvent.Marker -> {
                    markerList.add(Marker(accumulatedDelayMicros, event.label))
                    _markers.value = markerList
                }

                is TermEvent.Exit -> {
                    // Keep final frame, will transition to Ended state
                }

                is TermEvent.Eot -> {
                    // For live streams: reset to pre-init state
                    // For recordings: shouldn't occur
                }
            }

            // Update frame
            _frame.value = virtualTerminal.snapshot()
            println("[AsciinemaPlayer] playEvents: Frame updated, lines with content: ${_frame.value.lines.count { it.runs.isNotEmpty() }}")
        }
        println("[AsciinemaPlayer] playEvents: Playback loop ended")
    }
}
