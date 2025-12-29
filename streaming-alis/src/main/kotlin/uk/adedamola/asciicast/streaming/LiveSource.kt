package uk.adedamola.asciicast.streaming

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okio.ByteString
import uk.adedamola.asciicast.player.PlaybackSource
import uk.adedamola.asciicast.player.SourceMetadata
import uk.adedamola.asciicast.vt.TermEvent
import uk.adedamola.asciicast.vt.TimedTermEvent
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Live streaming source using ALiS v1 protocol over WebSocket.
 *
 * @param url WebSocket URL (e.g., "wss://asciinema.org/ws/s/TOKEN")
 */
class LiveSource(
    private val url: String
) : PlaybackSource {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for streaming
        .build()

    private val decoder = AlisDecoder()
    private val eventChannel = Channel<AlisEvent>(Channel.UNLIMITED)

    private var initEvent: TermEvent.Init? = null
    private var socket: WebSocket? = null

    override suspend fun init(): TermEvent.Init {
        // Connect to WebSocket and wait for Init event
        return suspendCoroutine { continuation ->
            val request = Request.Builder()
                .url(url)
                .build()

            val listener = object : WebSocketListener() {
                private var receivedMagic = false

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    socket = webSocket
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (!receivedMagic) {
                        // First message should be magic
                        if (decoder.verifyMagic(bytes.toByteArray())) {
                            receivedMagic = true
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Invalid ALiS magic bytes")
                            )
                            webSocket.close(1000, "Invalid magic")
                        }
                        return
                    }

                    // Decode event
                    try {
                        val event = decoder.decode(bytes.toByteArray())

                        when (event) {
                            is AlisEvent.Init -> {
                                // Convert to TermEvent.Init and resume
                                val termInit = TermEvent.Init(
                                    cols = event.cols,
                                    rows = event.rows,
                                    theme = event.theme,
                                    initData = event.initData
                                )
                                initEvent = termInit
                                continuation.resume(termInit)
                            }

                            else -> {
                                // Queue other events for the events() flow
                                eventChannel.trySend(event)
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                        webSocket.close(1000, "Decode error")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    continuation.resumeWithException(t)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    eventChannel.close()
                }
            }

            client.newWebSocket(request, listener)
        }
    }

    override fun events(): Flow<TimedTermEvent> = flow {
        var lastEventTimeMicros = 0L

        for (alisEvent in eventChannel) {
            val timedEvent = when (alisEvent) {
                is AlisEvent.Init -> {
                    // Subsequent Init (reconnection or reset)
                    lastEventTimeMicros = alisEvent.relTimeMicros
                    TimedTermEvent(
                        TermEvent.Init(
                            cols = alisEvent.cols,
                            rows = alisEvent.rows,
                            theme = alisEvent.theme,
                            initData = alisEvent.initData
                        ),
                        alisEvent.relTimeMicros
                    )
                }

                is AlisEvent.Output -> {
                    val event = TimedTermEvent(
                        TermEvent.Output(alisEvent.data),
                        alisEvent.relTimeMicros
                    )
                    lastEventTimeMicros += alisEvent.relTimeMicros
                    event
                }

                is AlisEvent.Resize -> {
                    val event = TimedTermEvent(
                        TermEvent.Resize(alisEvent.cols, alisEvent.rows),
                        alisEvent.relTimeMicros
                    )
                    lastEventTimeMicros += alisEvent.relTimeMicros
                    event
                }

                is AlisEvent.Marker -> {
                    val event = TimedTermEvent(
                        TermEvent.Marker(alisEvent.label),
                        alisEvent.relTimeMicros
                    )
                    lastEventTimeMicros += alisEvent.relTimeMicros
                    event
                }

                is AlisEvent.Exit -> {
                    val event = TimedTermEvent(
                        TermEvent.Exit(alisEvent.status),
                        alisEvent.relTimeMicros
                    )
                    lastEventTimeMicros += alisEvent.relTimeMicros
                    event
                }

                is AlisEvent.Eot -> {
                    // Reset state and wait for next Init
                    lastEventTimeMicros = 0L
                    TimedTermEvent(TermEvent.Eot, alisEvent.relTimeMicros)
                }
            }

            emit(timedEvent)
        }
    }

    override val metadata: SourceMetadata
        get() = SourceMetadata(
            duration = null, // Live streams have no fixed duration
            idleTimeLimit = null,
            title = "Live Stream",
            command = null,
            seekable = false
        )

    /**
     * Close the WebSocket connection.
     */
    fun close() {
        socket?.close(1000, "Client closing")
        client.dispatcher.executorService.shutdown()
        eventChannel.close()
    }
}
