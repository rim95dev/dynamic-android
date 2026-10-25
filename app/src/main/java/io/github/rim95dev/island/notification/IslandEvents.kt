package io.github.rim95dev.island.notification

import android.app.PendingIntent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class IslandNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String?,
    val icon: Drawable?,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,
)

sealed interface IslandEvent {
    data class Show(val notification: IslandNotification) : IslandEvent
    data class Remove(val key: String) : IslandEvent
}

/** Connects the notification listener and the accessibility service, which run in the same process. */
object IslandEvents {

    private val _events = MutableSharedFlow<IslandEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<IslandEvent> = _events.asSharedFlow()

    private val _cancelRequests = MutableSharedFlow<String>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Keys of notifications to remove after the user opened them from the island. */
    val cancelRequests: SharedFlow<String> = _cancelRequests.asSharedFlow()

    fun post(event: IslandEvent) {
        _events.tryEmit(event)
    }

    fun requestCancel(key: String) {
        _cancelRequests.tryEmit(key)
    }
}
