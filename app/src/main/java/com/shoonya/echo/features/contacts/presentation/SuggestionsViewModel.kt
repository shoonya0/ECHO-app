package com.shoonya.echo.features.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.contacts.domain.usecase.GetUserSuggestionsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.SendContactRequestUseCase
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
class SuggestionsViewModel @Inject constructor(
    private val getUserSuggestionsUseCase: GetUserSuggestionsUseCase,
    private val sendContactRequestUseCase: SendContactRequestUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestionsUiState())
    val state: StateFlow<SuggestionsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SuggestionsEvent>()
    val events: SharedFlow<SuggestionsEvent> = _events.asSharedFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        load()
    }

    fun onEvent(event: SuggestionsEvent) {
        when (event) {
            is SuggestionsEvent.Load -> load()
            is SuggestionsEvent.LoadMore -> loadMore()
            is SuggestionsEvent.SendRequest -> sendRequest(event.userId)
            is SuggestionsEvent.Refresh -> refresh()
            is SuggestionsEvent.ShowSnackbar -> { /* emitted internally */ }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, page = 1) }
            runCatching {
                val suggestions = getUserSuggestionsUseCase(1, PAGE_SIZE).getOrThrow()
                val hasMore = suggestions.size >= PAGE_SIZE
                _state.update {
                    it.copy(
                        isLoading = false,
                        suggestions = suggestions,
                        hasMore = hasMore,
                        page = 1,
                    )
                }
            }.onFailure { err ->
                Timber.tag("SuggestionsVM").e(err, "load failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = err.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            val nextPage = current.page + 1
            _state.update { it.copy(isLoadingMore = true) }
            runCatching {
                val more = getUserSuggestionsUseCase(nextPage, PAGE_SIZE).getOrThrow()
                val hasMore = more.size >= PAGE_SIZE
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        suggestions = it.suggestions + more,
                        hasMore = hasMore,
                        page = nextPage,
                    )
                }
            }.onFailure { err ->
                Timber.tag("SuggestionsVM").e(err, "loadMore failed")
                _state.update { it.copy(isLoadingMore = false) }
                _events.emit(SuggestionsEvent.ShowSnackbar(err.toUserMessage()))
            }
        }
    }

    private fun refresh() {
        load()
    }

    private fun sendRequest(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = userId) }
            sendContactRequestUseCase(userId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            suggestions = state.suggestions.filter { it.id != userId },
                        )
                    }
                    _events.emit(SuggestionsEvent.ShowSnackbar("Contact request sent"))
                }
                .onFailure { err ->
                    Timber.tag("SuggestionsVM").e(err, "sendRequest failed: %s", userId)
                    _events.emit(SuggestionsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }
}