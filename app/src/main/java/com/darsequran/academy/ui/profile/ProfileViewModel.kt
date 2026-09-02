package com.darsequran.academy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.UpdateProfileRequest
import com.darsequran.academy.data.model.UserDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserDto? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showEditDialog: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getProfile()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data.user
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

    fun openEditDialog() {
        _uiState.update { it.copy(showEditDialog = true, errorMessage = null, successMessage = null) }
    }

    fun closeEditDialog() {
        _uiState.update { it.copy(showEditDialog = false) }
    }

    fun updateProfile(
        name: String,
        fatherName: String,
        dateOfBirth: String,
        occupation: String,
        address: String,
        phone: String,
        gender: String,
        image: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, errorMessage = null) }

            val request = UpdateProfileRequest(
                name = name.trim().ifEmpty { null },
                fatherName = fatherName.trim().ifEmpty { null },
                dateOfBirth = dateOfBirth.trim().ifEmpty { null },
                occupation = occupation.trim().ifEmpty { null },
                address = address.trim().ifEmpty { null },
                whatsapp = phone.trim().ifEmpty { null },
                gender = gender.trim().ifEmpty { null },
                image = image?.trim()?.ifEmpty { null }
            )

            when (val result = authRepository.updateProfile(request)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            showEditDialog = false,
                            user = result.data.user ?: it.user,
                            successMessage = "Profile updated successfully!"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(authRepository) as T
        }
    }
}
