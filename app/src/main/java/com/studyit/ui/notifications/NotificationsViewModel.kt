package com.studyit.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.AppNotification
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val unread: Int = 0
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()

    init {
        viewModelScope.launch {
            notificationRepository.observe(uid).collect { list ->
                _state.update {
                    it.copy(
                        loading = false,
                        notifications = list,
                        unread = list.count { notification -> !notification.isRead }
                    )
                }
            }
        }
    }

    fun markRead(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch { notificationRepository.markRead(notification.notificationId) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead(uid) }
    }

    fun clearAll() {
        viewModelScope.launch { notificationRepository.clearAll(uid) }
    }
}
