package com.shoonya.echo.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.settings.domain.usecase.GetProfileUseCase
import com.shoonya.echo.features.settings.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditProfileEvent>()
    val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    fun onFieldChanged(field: String, value: String) {
        _state.update {
            when (field) {
                "displayName" -> it.copy(displayName = value, displayNameError = null)
                "avatar" -> it.copy(avatar = value)
                "statusMessage" -> it.copy(statusMessage = value)
                "bio" -> it.copy(bio = value)
                else -> it
            }
        }
    }

    fun save() {
        val current = _state.value

        if (current.displayName.isBlank()) {
            _state.update { it.copy(displayNameError = "Display name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            updateProfileUseCase(
                displayName = current.displayName.takeIf { it.isNotBlank() },
                avatar = current.avatar.takeIf { it.isNotBlank() },
                statusMessage = current.statusMessage.takeIf { it.isNotBlank() },
                bio = current.bio.takeIf { it.isNotBlank() },
            )
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(EditProfileEvent.Saved)
                }
                .onFailure { err ->
                    Timber.tag("EditProfileVM").e(err, "save failed")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getProfileUseCase()
                .onSuccess { profile ->
                    _state.update {
                        EditProfileUiState(
                            isLoading = false,
                            displayName = profile.displayName,
                            avatar = profile.avatar,
                            statusMessage = profile.statusMessage,
                            bio = profile.bio,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.tag("EditProfileVM").e(err, "loadProfile failed")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }
}

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val displayName: String = "",
    val avatar: String = "",
    val statusMessage: String = "",
    val bio: String = "",
    val error: String? = null,
    val displayNameError: String? = null,
)

sealed interface EditProfileEvent {
    data object Saved : EditProfileEvent
}