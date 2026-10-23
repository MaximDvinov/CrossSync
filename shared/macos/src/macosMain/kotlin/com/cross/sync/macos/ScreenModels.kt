package com.cross.sync.macos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.cross.sync.clipboard.presentation.ClipboardState
import com.cross.sync.clipboard.presentation.ClipboardViewModel
import com.cross.sync.clipboard.presentation.model.CategoryStable
import com.cross.sync.clipboard.presentation.model.CopiedDataStable
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.presentation.NotificationState
import com.cross.sync.notifications.presentation.NotificationViewModel
import com.cross.sync.setting.presentation.DesktopSettingViewModel
import com.cross.sync.setting.presentation.SettingState
import com.cross.sync.setting.domain.DesktopNotificationDelivery
import com.cross.sync.syncing.domain.entity.PairingState
import com.cross.sync.theme.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StateSubscription internal constructor(private val job: Job) {
    fun cancel() { job.cancel() }
}

internal class ScreenOwner(private val viewModel: ViewModel) {
    private val store = ViewModelStore().apply { put("screen", viewModel) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun <T> watch(state: StateFlow<T>, receive: (T) -> Unit): StateSubscription {
        receive(state.value)
        return StateSubscription(scope.launch { state.collect { receive(it) } })
    }
    fun close() { scope.cancel(); store.clear() }
    suspend fun awaitClosed() {
        scope.coroutineContext[Job]?.join()
        viewModel.viewModelScope.coroutineContext[Job]?.join()
    }
}

class ClipboardScreenModel internal constructor(private val model: ClipboardViewModel, private val owner: ScreenOwner) {
    val current: ClipboardState get() = model.state.value
    fun watch(receive: (ClipboardState) -> Unit) = owner.watch(model.state, receive)
    fun selectCategory(id: Long) {
        model.selectCategory(when (id) {
            -1L -> null
            -2L -> CategoryStable.UNCATEGORIZED
            else -> current.categories.firstOrNull { it.id == id }
        })
    }
    fun copy(id: Long, completed: (String?) -> Unit) {
        val item = current.copiedDataList.firstOrNull { it.id == id } ?: return
        owner.scope.launch {
            try {
                model.copyCopiedData(item)
                completed(null)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                completed(error.message ?: "Unable to copy this item")
            }
        }
    }
    fun delete(id: Long) { current.copiedDataList.firstOrNull { it.id == id }?.let(model::deleteCopiedData) }
    fun moveToCategory(id: Long, categoryId: Long) {
        current.copiedDataList.firstOrNull { it.id == id }?.let { model.moveCopiedDataToCategory(categoryId, it) }
    }
    fun clear() = model.clearAll()
    fun close() = owner.close()
}

class SettingsScreenModel internal constructor(private val model: DesktopSettingViewModel, private val owner: ScreenOwner) {
    val current: SettingState get() = model.state.value
    val qrCode: String? get() = (current.pairingState as? PairingState.QrCodeGenerated)?.qrCode
    val pairingConnected: Boolean get() = current.pairingState is PairingState.Connected
    val pairingError: String? get() = (current.pairingState as? PairingState.Error)?.message
    fun watch(receive: (SettingState) -> Unit) = owner.watch(model.state, receive)
    fun setTheme(value: String) = model.setThemeMode(AppThemeMode.fromStoredValue(value))
    fun setDelivery(value: String) = model.setDesktopNotificationDelivery(DesktopNotificationDelivery.fromStoredValue(value))
    fun toggleAutoStart() = model.toggleLaunchAtSystemStartup()
    fun setAutoStart(enabled: Boolean) = model.setLaunchAtSystemStartup(enabled)
    fun setRetention(days: Int) = model.setClipboardAutoClearTimeout(days)
    fun setQuickSize(size: Int) = model.setQuickAccessHistorySize(size)
    fun setNotificationRetention(days: Int) = model.setNotificationHistoryRetention(days)
    fun addCategory(name: String) = model.addCategory(name)
    fun renameCategory(id: Long, name: String) = model.renameCategory(id, name)
    fun deleteCategory(id: Long) = model.deleteCategory(id)
    fun exclude(id: String) = model.addExcludedApplication(id)
    fun include(id: String) = model.removeExcludedApplication(id)
    fun pair() = model.pairingDevice()
    fun cancelPairing() = model.qrCodeCancel()
    fun unpair(id: String) = model.unpairDevice(id)
    fun close() { model.qrCodeCancel(); owner.close() }
}

class NotificationsScreenModel internal constructor(private val model: NotificationViewModel, private val owner: ScreenOwner) {
    val current: NotificationState get() = model.state.value
    fun watch(receive: (NotificationState) -> Unit) = owner.watch(model.state, receive)
    fun dismiss(notification: SyncedNotification) = model.dismiss(notification)
    fun open(notification: SyncedNotification) = model.open(notification)
    fun invoke(notification: SyncedNotification, index: Int) = model.invoke(notification, index)
    fun reply(notification: SyncedNotification, index: Int, text: String) = model.reply(notification, index, text)
    fun clear() = model.clearHistory()
    fun close() = owner.close()
}

object NativePresentation {
    fun date(item: CopiedDataStable): String = item.date.toString().replace('T', ' ').substringBefore('.')
    fun kind(item: CopiedDataStable): String = when (item) {
        is CopiedDataStable.Text -> "text"
        is CopiedDataStable.FormattedText -> item.mimeType.substringAfter('/')
        is CopiedDataStable.Image -> "image"
        is CopiedDataStable.File -> "files"
    }
    fun text(item: CopiedDataStable): String = when (item) {
        is CopiedDataStable.Text -> item.text
        is CopiedDataStable.FormattedText -> item.plainText
        is CopiedDataStable.Image -> item.imagePath
        is CopiedDataStable.File -> item.filePaths.joinToString("\n")
    }
    fun path(item: CopiedDataStable): String? = (item as? CopiedDataStable.Image)?.imagePath
    fun notificationId(notification: SyncedNotification) = "${notification.deviceId}:${notification.notificationKey}"
}
