package com.cross.sync

import android.app.Notification
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.util.Base64
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.cross.sync.notifications.domain.entity.NotificationAction
import com.cross.sync.notifications.domain.entity.NotificationActionDescriptor
import com.cross.sync.notifications.domain.entity.NotificationActionRequest
import com.cross.sync.notifications.domain.entity.NotificationKind
import com.cross.sync.notifications.domain.entity.NotificationMedia
import com.cross.sync.notifications.domain.entity.NotificationRemoval
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.repository.NotificationActionExecutor
import com.cross.sync.notifications.domain.repository.NotificationPublisher
import com.cross.sync.notifications.domain.repository.NotificationSnapshotPublisher
import com.cross.sync.setting.domain.SettingPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import java.util.concurrent.ConcurrentHashMap

class AndroidNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        instance = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            eventMutex.withLock {
                if (!isSyncEnabled()) {
                    clearTrackedNotifications()
                    return@withLock
                }
                refreshTrackedNotifications()
                reconcileAll(force = true)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            eventMutex.withLock {
                if (!isSyncEnabled()) {
                    clearTrackedNotifications()
                    return@withLock
                }
                trackNotification(sbn)
                val groupKey = sbn.notificationGroupKey()
                trackActiveGroup(groupKey)
                reconcileGroup(groupKey)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        serviceScope.launch {
            eventMutex.withLock {
                active.remove(sbn.key)
                removePublished(sbn.key)
                if (!isSyncEnabled()) {
                    clearTrackedNotifications()
                    return@withLock
                }
                val groupKey = sbn.notificationGroupKey()
                trackActiveGroup(groupKey)
                reconcileGroup(groupKey)
            }
        }
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun trackNotification(sbn: StatusBarNotification) {
        if (!shouldTrack(sbn)) {
            active.remove(sbn.key)
            removePublished(sbn.key)
            return
        }
        active[sbn.key] = sbn
    }

    private fun shouldTrack(sbn: StatusBarNotification): Boolean {
        return sbn.user == Process.myUserHandle() &&
            sbn.packageName != packageName &&
            sbn.packageName !in GlobalContext.get().get<SettingPreferencesStore>().getExcludedApplicationIds()
    }

    private suspend fun reconcileAll(force: Boolean) {
        active.values
            .groupBy { it.notificationGroupKey() }
            .values
            .forEach { notifications -> reconcile(notifications, force) }
    }

    private suspend fun trackActiveGroup(groupKey: String) {
        for (notification in activeNotifications.orEmpty()) {
            if (notification.notificationGroupKey() == groupKey) {
                trackNotification(notification)
            }
        }
    }

    private suspend fun refreshTrackedNotifications() {
        val currentNotifications = activeNotifications.orEmpty()
        val currentKeys = currentNotifications.mapTo(mutableSetOf()) { it.key }
        for (notificationKey in active.keys) {
            if (notificationKey !in currentKeys) {
                active.remove(notificationKey)
                removePublished(notificationKey)
            }
        }
        for (notification in currentNotifications) {
            trackNotification(notification)
        }
    }

    private suspend fun reconcileGroup(groupKey: String) {
        reconcile(
            notifications = active.values.filter { it.notificationGroupKey() == groupKey },
            force = false,
        )
    }

    private suspend fun reconcile(
        notifications: List<StatusBarNotification>,
        force: Boolean,
    ) {
        val hasChildren = notifications.any { !it.isGroupSummary() }
        notifications.forEach { notification ->
            if (notification.isGroupSummary() && hasChildren) {
                removePublished(notification.key)
            } else {
                publishIfChanged(notification, force)
            }
        }
    }

    private suspend fun publishIfChanged(
        sbn: StatusBarNotification,
        force: Boolean,
    ) {
        val notification = sbn.toSyncedNotification(
            context = applicationContext,
            includeContent = notificationSettings().notificationContentEnabled,
        )
        val previous = published[sbn.key]
        if (!force && previous?.copy(updatedAt = 0) == notification.copy(updatedAt = 0)) return

        GlobalContext.get().get<NotificationPublisher>().publish(notification)
            .onSuccess { published[sbn.key] = notification }
    }

    private suspend fun removePublished(notificationKey: String) {
        if (published[notificationKey] == null) return
        GlobalContext.get().get<NotificationPublisher>()
            .remove(NotificationRemoval(notificationKey, System.currentTimeMillis()))
            .onSuccess { published.remove(notificationKey) }
    }

    private suspend fun clearTrackedNotifications() {
        for (notificationKey in published.keys.toList()) {
            removePublished(notificationKey)
        }
        active.clear()
    }

    private fun isSyncEnabled(): Boolean = notificationSettings().notificationSyncEnabled

    private suspend fun publishActiveNotifications() {
        eventMutex.withLock {
            if (!isSyncEnabled()) {
                clearTrackedNotifications()
            } else {
                refreshTrackedNotifications()
                reconcileAll(force = true)
            }
        }
    }

    private fun notificationSettings() = GlobalContext.get()
        .get<SettingPreferencesStore>()
        .getGeneralSettings()

    companion object {
        private val active = ConcurrentHashMap<String, StatusBarNotification>()
        private val published = ConcurrentHashMap<String, SyncedNotification>()
        private val eventMutex = Mutex()
        @Volatile
        private var instance: AndroidNotificationListener? = null
        private var appContext: Context? = null

        internal fun execute(request: NotificationActionRequest): Result<Unit> = runCatching {
            val context = appContext ?: error("Notification listener is unavailable")
            val listener = instance
            val notification = if (listener != null) {
                listener.activeNotifications
                    ?.firstOrNull { it.key == request.notificationKey }
                    ?.also { active[it.key] = it }
                    ?: error("Notification is no longer active")
            } else {
                active[request.notificationKey]
                    ?: error("Notification is no longer active")
            }

            when (val action = request.action) {
                NotificationAction.Open -> notification.notification.contentIntent?.send(context, 0, null)
                    ?: error("Notification cannot be opened")

                NotificationAction.Dismiss -> {
                    instance?.cancelNotification(notification.key)
                        ?: error("Notification listener is unavailable")
                    active.remove(notification.key)
                }

                is NotificationAction.Invoke -> notification.actionAt(action.index).actionIntent
                    ?.send(context, 0, null)
                    ?: error("Notification action is unavailable")

                is NotificationAction.Reply -> {
                    val notificationAction = notification.actionAt(action.index)
                    val remoteInput = notificationAction.remoteInputs
                        ?.firstOrNull { it.allowFreeFormInput }
                        ?: error("Reply is not supported")
                    val fillInIntent = Intent()
                    android.app.RemoteInput.addResultsToIntent(
                        arrayOf(remoteInput),
                        fillInIntent,
                        Bundle().apply {
                            putCharSequence(remoteInput.resultKey, action.text)
                        }
                    )
                    notificationAction.actionIntent?.send(context, 0, fillInIntent)
                        ?: error("Notification action is unavailable")
                }
            }
        }

        internal suspend fun publishActive() {
            instance?.publishActiveNotifications()
        }

        private fun StatusBarNotification.actionAt(index: Int): Notification.Action {
            return notification.actions?.getOrNull(index)
                ?: error("Notification action is no longer available")
        }
    }
}

class AndroidNotificationActionExecutor : NotificationActionExecutor {
    override suspend fun execute(request: NotificationActionRequest): Result<Unit> {
        return AndroidNotificationListener.execute(request)
    }
}

class AndroidNotificationSnapshotPublisher : NotificationSnapshotPublisher {
    override suspend fun publishActive() {
        AndroidNotificationListener.publishActive()
    }
}

private fun StatusBarNotification.notificationGroupKey(): String {
    return groupKey.orEmpty()
        .takeIf { it.isNotBlank() }
        ?.let { "$packageName:$it" }
        .orEmpty()
}

private fun StatusBarNotification.isGroupSummary(): Boolean {
    return notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
}

private fun StatusBarNotification.toSyncedNotification(
    context: Context,
    includeContent: Boolean,
): SyncedNotification {
    val preview = notification.preview()
    val sourcePackage = packageName
    val appName = runCatching {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(sourcePackage, 0)
        ).toString()
    }.getOrDefault(sourcePackage)
    return SyncedNotification(
        notificationKey = key,
        packageName = sourcePackage,
        appName = appName,
        title = if (includeContent) preview.title else "New notification",
        body = if (includeContent) preview.body else "Preview hidden on phone",
        shortCriticalText = notification.shortCriticalText(),
        postedAt = postTime,
        updatedAt = System.currentTimeMillis(),
        isOngoing = isOngoing,
        groupKey = notificationGroupKey(),
        isGroupSummary = isGroupSummary(),
        kind = preview.kind,
        hasPreview = includeContent && preview.hasContent,
        media = notification.media(context, sourcePackage, preview.kind, includeContent),
        actions = notification.actions.orEmpty().mapIndexedNotNull { index, action ->
            action.actionIntent ?: return@mapIndexedNotNull null
            NotificationActionDescriptor(
                index = index,
                title = action.title?.toString().orEmpty().ifBlank { "Action" },
                supportsReply = action.remoteInputs.orEmpty().any { it.allowFreeFormInput },
            )
        },
    )
}

private fun Notification.media(
    context: Context,
    sourcePackage: String,
    kind: NotificationKind,
    includeContent: Boolean,
): NotificationMedia? {
    val appIcon = runCatching {
        context.packageManager.getApplicationIcon(sourcePackage).toBase64(maxDimension = 96)
    }.getOrNull()
    val notificationIcon = runCatching {
        smallIcon.loadDrawable(context)?.toBase64(maxDimension = 96)
    }.getOrNull()
    if (!includeContent) {
        return NotificationMedia(
            appIcon = appIcon,
            notificationIcon = notificationIcon,
        ).takeIf { it.appIcon != null || it.notificationIcon != null }
    }

    val avatar = if (kind == NotificationKind.Message) {
        messageAvatar(context)?.toBase64(maxDimension = 128)
    } else {
        null
    }
    val image = picture()?.toBase64(
        maxDimension = 640,
        format = Bitmap.CompressFormat.JPEG,
        quality = 84,
    )
    return NotificationMedia(
        appIcon = appIcon,
        notificationIcon = notificationIcon,
        image = image,
        avatar = avatar,
    ).takeIf { it.appIcon != null || it.notificationIcon != null || it.image != null || it.avatar != null }
}

@Suppress("DEPRECATION")
private fun Notification.messageAvatar(context: Context): Drawable? {
    val senderAvatar = extras.messageBundles(Notification.EXTRA_MESSAGES)
        .asReversed()
        .asSequence()
        .mapNotNull { it.getParcelable<Person>("sender_person")?.icon?.loadDrawable(context) }
        .firstOrNull()
    return senderAvatar
        ?: extras.drawable(context, Notification.EXTRA_LARGE_ICON_BIG)
        ?: extras.drawable(context, Notification.EXTRA_LARGE_ICON)
}

@Suppress("DEPRECATION")
private fun Notification.picture(): Bitmap? {
    return extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
}

@Suppress("DEPRECATION")
private fun Bundle.drawable(context: Context, key: String): Drawable? {
    return getParcelable<android.graphics.drawable.Icon>(key)?.loadDrawable(context)
        ?: getParcelable<Bitmap>(key)?.let { BitmapDrawable(context.resources, it) }
}

private fun Drawable.toBase64(maxDimension: Int): String? {
    val width = intrinsicWidth.takeIf { it > 0 } ?: maxDimension
    val height = intrinsicHeight.takeIf { it > 0 } ?: maxDimension
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
    val bitmap = Bitmap.createBitmap(
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    Canvas(bitmap).also { canvas ->
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }
    return bitmap.toBase64(maxDimension)
}

private fun Bitmap.toBase64(
    maxDimension: Int,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100,
): String? {
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    } else {
        this
    }
    return ByteArrayOutputStream().use { output ->
        if (!scaled.compress(format, quality, output)) return null
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}

private data class NotificationPreview(
    val title: String,
    val body: String,
    val kind: NotificationKind,
    val hasContent: Boolean,
)

@Suppress("DEPRECATION")
private fun Notification.preview(): NotificationPreview {
    val messages = extras.messages(Notification.EXTRA_MESSAGES)
    val historicMessages = extras.messages(Notification.EXTRA_HISTORIC_MESSAGES)
    val inboxLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        .orEmpty()
        .mapNotNull { it.asNotificationText().takeIf(String::isNotBlank) }
    val title = sequenceOf(
        extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
        extras.getCharSequence(Notification.EXTRA_TITLE),
        extras.getCharSequence("android.conversationTitle"),
    )
        .mapNotNull { it?.asNotificationText()?.takeIf(String::isNotBlank) }
        .firstOrNull()
        .orEmpty()
    val body = sequenceOf(
        messages.lastOrNull() ?: historicMessages.lastOrNull(),
        inboxLines.takeIf { it.isNotEmpty() }?.joinToString("\n"),
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.asNotificationText(),
        extras.getCharSequence(Notification.EXTRA_TEXT)?.asNotificationText(),
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.asNotificationText(),
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.asNotificationText(),
        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.asNotificationText(),
        tickerText?.asNotificationText(),
    )
        .firstOrNull { !it.isNullOrBlank() }
        .orEmpty()
    val hasContent = title.isNotBlank() || body.isNotBlank()
    return NotificationPreview(
        title = title.ifBlank { "New notification" },
        body = body,
        kind = kind(),
        hasContent = hasContent,
    )
}

private fun Notification.shortCriticalText(): String {
    return extras.getCharSequence(SHORT_CRITICAL_TEXT_EXTRA)
        ?.toString()
        ?.trim()
        ?.take(MAX_SHORT_CRITICAL_TEXT_LENGTH)
        .orEmpty()
}

@Suppress("DEPRECATION")
private fun Bundle.messages(key: String): List<String> {
    return messageBundles(key)
        .mapNotNull { message ->
            val text = message.getCharSequence("text")?.asNotificationText().orEmpty()
            val sender = message.getCharSequence("sender")?.asNotificationText().orEmpty()
            text.takeIf { it.isNotBlank() }?.let { if (sender.isBlank()) it else "$sender: $it" }
        }
}

@Suppress("DEPRECATION")
private fun Bundle.messageBundles(key: String): List<Bundle> {
    return getParcelableArray(key)
        .orEmpty()
        .mapNotNull { it as? Bundle }
}

@Suppress("DEPRECATION")
private fun Notification.kind(): NotificationKind {
    val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
    return when {
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.isNotEmpty() == true ||
            template.contains("MessagingStyle") -> NotificationKind.Message
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.isNotEmpty() == true ||
            template.contains("InboxStyle") -> NotificationKind.Inbox
        category == Notification.CATEGORY_CALL || template.contains("CallStyle") -> NotificationKind.Call
        category == Notification.CATEGORY_TRANSPORT || template.contains("MediaStyle") -> NotificationKind.Media
        extras.containsKey(Notification.EXTRA_PROGRESS) || template.contains("ProgressStyle") -> NotificationKind.Progress
        template.contains("BigPictureStyle") -> NotificationKind.Image
        contentView != null || bigContentView != null || headsUpContentView != null -> NotificationKind.Custom
        else -> NotificationKind.Other
    }
}

private fun CharSequence.asNotificationText(): String = toString().trim().take(1_000)

private const val SHORT_CRITICAL_TEXT_EXTRA = "android.shortCriticalText"
private const val MAX_SHORT_CRITICAL_TEXT_LENGTH = 7
