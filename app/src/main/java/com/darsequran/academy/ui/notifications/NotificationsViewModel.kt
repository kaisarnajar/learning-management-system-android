package com.darsequran.academy.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.NotificationDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFilter: String = "all", // "all" or "unread"
    val selectedNotification: NotificationDto? = null
)

class NotificationsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        fetchNotifications()
    }

    fun fetchNotifications(filter: String = _uiState.value.selectedFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedFilter = filter) }
            when (val result = authRepository.getNotifications(filter = filter)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = result.data.data ?: emptyList(),
                            unreadCount = result.data.unreadCount ?: 0
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun setFilter(filter: String) {
        if (_uiState.value.selectedFilter != filter) {
            fetchNotifications(filter)
        }
    }

    fun selectNotification(notification: NotificationDto) {
        _uiState.update { it.copy(selectedNotification = notification) }
        if (!notification.isRead) {
            markAsRead(notification.id)
        }
    }

    fun dismissNotificationDetail() {
        _uiState.update { it.copy(selectedNotification = null) }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (authRepository.markNotificationRead(notificationId)) {
                is NetworkResult.Success -> {
                    // Update local state optimistically
                    _uiState.update { state ->
                        val updatedList = state.notifications.map { n ->
                            if (n.id == notificationId) n.copy(isRead = true) else n
                        }
                        val newUnread = (state.unreadCount - 1).coerceAtLeast(0)
                        state.copy(notifications = updatedList, unreadCount = newUnread)
                    }
                }
                else -> {}
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (authRepository.markAllNotificationsRead()) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.notifications.map { n -> n.copy(isRead = true) }
                        state.copy(notifications = updatedList, unreadCount = 0)
                    }
                }
                else -> {}
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationsViewModel(authRepository) as T
        }
    }
}
