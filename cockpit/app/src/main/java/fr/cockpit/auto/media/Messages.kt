package fr.cockpit.auto.media

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import fr.cockpit.auto.apps.Launchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Un message reçu (SMS, WhatsApp, Messenger, Signal…), lu dans sa notification. */
data class Message(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val sender: String,
    val text: String,
    val time: Long,
    val reply: Notification.Action?,
    val open: PendingIntent?,
)

/** Les derniers messages reçus pendant que Cockpit tourne. */
object Messages {
    private const val MAX = 30

    private val _list = MutableStateFlow<List<Message>>(emptyList())
    val list: StateFlow<List<Message>> = _list.asStateFlow()

    /** Une conversation n'apparaît qu'une fois, avec son dernier message. */
    fun add(m: Message) {
        _list.value = (listOf(m) + _list.value.filter { it.key != m.key }).take(MAX)
    }

    fun clear() {
        _list.value = emptyList()
    }

    /** Transforme une notification en message, ou null si ce n'en est pas un. */
    fun parse(ctx: Context, sbn: StatusBarNotification): Message? {
        if (sbn.packageName == ctx.packageName || sbn.isOngoing) return null
        val n = sbn.notification ?: return null
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = n.extras ?: return null
        val reply = n.actions?.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
        val isMessage = n.category == Notification.CATEGORY_MESSAGE ||
            extras.containsKey(Notification.EXTRA_MESSAGES) || reply != null
        if (!isMessage) return null
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.takeIf { it.isNotBlank() }
            ?: return null
        val sender = (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.takeIf { it.isNotBlank() }
            ?: Launchers.label(ctx, sbn.packageName)
        return Message(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = Launchers.label(ctx, sbn.packageName),
            sender = sender,
            text = text,
            time = sbn.postTime,
            reply = reply,
            open = n.contentIntent,
        )
    }

    /** Répond directement depuis Cockpit, comme depuis la notification. */
    fun reply(ctx: Context, m: Message, text: String): Boolean {
        val action = m.reply ?: return false
        val inputs = action.remoteInputs ?: return false
        val results = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, text) } }
        val intent = Intent()
        RemoteInput.addResultsToIntent(inputs, intent, results)
        return try {
            action.actionIntent.send(ctx, 0, intent)
            true
        } catch (e: PendingIntent.CanceledException) {
            false
        }
    }
}
