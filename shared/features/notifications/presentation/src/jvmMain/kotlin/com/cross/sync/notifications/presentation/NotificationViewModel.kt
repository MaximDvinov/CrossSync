package com.cross.sync.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cross.sync.notifications.domain.entity.NotificationAction
import com.cross.sync.notifications.domain.entity.NotificationActionRequest
import com.cross.sync.notifications.domain.entity.SyncedNotification
import com.cross.sync.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cross.sync.notifications.domain.usecase.DismissNotificationUseCase
import com.cross.sync.notifications.domain.usecase.ClearNotificationHistoryUseCase
import com.cross.sync.notifications.domain.usecase.ObserveNotificationsUseCase
import com.cross.sync.syncing.domain.usecases.SendNotificationActionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class NotificationState(
    val notifications: List<SyncedNotification> = emptyList(),
    val actionError: String? = null,
    val pendingActions: Set<String> = emptySet(),
)

class NotificationViewModel(
    observeNotifications: ObserveNotificationsUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val clearNotificationHistory: ClearNotificationHistoryUseCase,
    private val sendAction: SendNotificationActionUseCase,
    private val dismissLocally: DismissNotificationUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeNotifications().collect { notifications ->
                _state.update { it.copy(notifications = notifications) }
            }
        }
    }

    fun dismiss(notification: SyncedNotification) = dispatch(notification, NotificationAction.Dismiss)

    fun invoke(notification: SyncedNotification, index: Int) = dispatch(
        notification,
        NotificationAction.Invoke(index),
    )

    fun reply(notification: SyncedNotification, index: Int, text: String) {
        if (text.isBlank()) return
        dispatch(notification, NotificationAction.Reply(index, text.trim()))
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                clearNotificationHistory()
                _state.update { it.copy(notifications = emptyList(), actionError = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(actionError = error.message ?: "Failed to clear notifications") }
            }
        }
    }

    private fun dispatch(notification: SyncedNotification, action: NotificationAction) {
        val key = notificationKey(notification)
        if (key in _state.value.pendingActions) return
        _state.update { it.copy(pendingActions = it.pendingActions + key, actionError = null) }
        viewModelScope.launch {
            try {
                if (action == NotificationAction.Dismiss) {
                    dismissLocally(notification.deviceId, notification.notificationKey)
                } else {
                    markRead(notification.deviceId, notification.notificationKey)
                }
                val result = sendAction(
                    NotificationActionRequest(
                        deviceId = notification.deviceId,
                        notificationKey = notification.notificationKey,
                        action = action,
                    )
                )
                // Local dismissal is complete even when the phone cannot be reached.
                // Call/reply failures must remain visible so the user can retry.
                if (action != NotificationAction.Dismiss) {
                    _state.update { it.copy(actionError = result.exceptionOrNull()?.message) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(actionError = error.message ?: "Failed to execute notification action") }
            } finally {
                _state.update { it.copy(pendingActions = it.pendingActions - key) }
            }
        }
    }

    private fun notificationKey(notification: SyncedNotification): String =
        "${notification.deviceId}:${notification.notificationKey}"
}
