package fr.cockpit.auto.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.service.media.MediaBrowserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LibraryItem(val id: String, val title: String, val subtitle: String, val browsable: Boolean, val playable: Boolean)

sealed interface LibraryState {
    data object Connecting : LibraryState
    /** L'app ne partage pas sa bibliothèque avec Cockpit (cas de Spotify par exemple). */
    data object Unavailable : LibraryState
    data class Items(val title: String, val items: List<LibraryItem>, val canGoBack: Boolean) : LibraryState
}

/**
 * Bibliothèque de l'app de musique, lue comme le fait Android Auto (MediaBrowser).
 * Certaines apps la réservent à Android Auto : on l'indique alors simplement.
 */
class Library(private val ctx: Context, private val pkg: String) {
    private val _state = MutableStateFlow<LibraryState>(LibraryState.Connecting)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var browser: MediaBrowser? = null
    private val stack = ArrayDeque<Pair<String, String>>() // (id, titre)

    fun connect() {
        // L'Android simulé des tests ne sait pas se connecter au service d'une autre app.
        if (android.os.Build.FINGERPRINT == "robolectric") return unavailable()
        val service = ctx.packageManager
            .queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE).setPackage(pkg), 0)
            .firstOrNull()?.serviceInfo
        if (service == null) {
            _state.value = LibraryState.Unavailable
            return
        }
        val b = MediaBrowser(ctx, ComponentName(service.packageName, service.name), object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                val root = browser?.root ?: return unavailable()
                stack.clear()
                load(root, "Bibliothèque")
            }

            override fun onConnectionFailed() = unavailable()
            override fun onConnectionSuspended() = unavailable()
        }, null)
        browser = b
        runCatching { b.connect() }.onFailure { unavailable() }
    }

    fun disconnect() {
        runCatching { browser?.disconnect() }
        browser = null
    }

    fun open(item: LibraryItem) {
        val b = browser ?: return
        if (item.browsable) {
            load(item.id, item.title)
        } else if (item.playable) {
            runCatching { MediaController(ctx, b.sessionToken).transportControls.playFromMediaId(item.id, null) }
        }
    }

    fun back() {
        if (stack.size < 2) return
        stack.removeLast()
        val (id, title) = stack.removeLast()
        load(id, title)
    }

    private fun load(id: String, title: String) {
        val b = browser ?: return unavailable()
        stack.addLast(id to title)
        runCatching { b.unsubscribe(id) }
        b.subscribe(id, object : MediaBrowser.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
                val items = children.map {
                    LibraryItem(
                        id = it.mediaId ?: "",
                        title = it.description.title?.toString() ?: "Sans titre",
                        subtitle = it.description.subtitle?.toString() ?: "",
                        browsable = it.isBrowsable,
                        playable = it.isPlayable,
                    )
                }.filter { it.id.isNotEmpty() }
                _state.value = if (items.isEmpty() && stack.size <= 1) LibraryState.Unavailable
                else LibraryState.Items(title, items, canGoBack = stack.size > 1)
            }

            override fun onError(parentId: String) = unavailable()
        })
    }

    private fun unavailable() {
        _state.value = LibraryState.Unavailable
    }
}
