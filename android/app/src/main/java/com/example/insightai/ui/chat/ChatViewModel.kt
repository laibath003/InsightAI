
package com.example.insightai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightai.data.model.ChatMessage
import com.example.insightai.data.model.DocumentSource
import com.example.insightai.data.model.Source
import com.example.insightai.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ResearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeDocumentIds: List<String> = emptyList()
    private var activeResearchJob: Job? = null
    private var sessionVersion = 0

    fun setActiveDocuments(documentIds: List<String>) {
        activeDocumentIds = documentIds.distinct()
    }

    fun newChat() {
        // Invalidate callbacks from any previous request.
        sessionVersion++

        activeResearchJob?.cancel()
        activeResearchJob = null

        activeDocumentIds = emptyList()
        _uiState.value = ChatUiState()
    }

    fun stopGenerating() {
        activeResearchJob?.cancel()
    }

    fun sendMessage(message: String, mode: String = "WEB") {
        if (message.isBlank() || _uiState.value.isLoading) return

        val documentIds = activeDocumentIds
        val normalizedMode = mode.uppercase()

        if (normalizedMode == "DOCUMENT" && documentIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Please upload at least one PDF for document research."
            )
            return
        }

        val requestMode = when {
            normalizedMode == "BOTH" && documentIds.isEmpty() -> "WEB"
            else -> normalizedMode
        }

        val generation = sessionVersion

        val userMessage = ChatMessage(
            message = message,
            isUser = true
        )

        val placeholder = ChatMessage(
            message = "Researching sources...",
            isUser = false,
            researchMode = requestMode
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage + placeholder,
            isLoading = true,
            error = null
        )

        activeResearchJob = viewModelScope.launch {
            val answer = StringBuilder()
            var currentMode = requestMode
            var webSources: List<Source> = emptyList()
            var documentSources: List<DocumentSource> = emptyList()

            try {
                repository.streamResearch(
                    message = message,
                    mode = requestMode,
                    documentIds = if (requestMode == "WEB") {
                        emptyList()
                    } else {
                        documentIds
                    },

                    onStatus = { status ->
                        if (generation == sessionVersion && answer.isEmpty()) {
                            updateAssistantMessage(
                                message = status,
                                researchMode = currentMode
                            )
                        }
                    },

                    onMode = { selectedMode ->
                        if (generation == sessionVersion) {
                            currentMode = selectedMode
                            updateAssistantMessage(
                                message = answer.toString().ifEmpty {
                                    "Generating answer..."
                                },
                                researchMode = currentMode
                            )
                        }
                    },

                    onChunk = { chunk ->
                        if (generation == sessionVersion) {
                            answer.append(chunk)
                            updateAssistantMessage(
                                message = answer.toString(),
                                researchMode = currentMode,
                                sources = webSources
                            )
                        }
                    },

                    onSources = { sources, docs ->
                        webSources = sources
                        documentSources = docs
                    }
                )

                if (generation != sessionVersion) return@launch

                val finalAnswer = buildString {
                    append(answer.toString())

                    if (documentSources.isNotEmpty()) {
                        append("\n\n**Document sources**\n")

                        documentSources.forEach { source ->
                            append(
                                "- ${source.title} " +
                                        "(section ${source.chunkIndex + 1})\n"
                            )
                        }
                    }
                }

                updateAssistantMessage(
                    message = finalAnswer.ifBlank {
                        "No answer was returned."
                    },
                    researchMode = currentMode,
                    sources = webSources
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

            } catch (e: CancellationException) {
                if (generation == sessionVersion) {
                    updateAssistantMessage(
                        message = answer.toString().ifBlank {
                            "Generation stopped."
                        },
                        researchMode = currentMode,
                        sources = webSources
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                }

                throw e

            } catch (e: Exception) {
                if (generation != sessionVersion) return@launch

                val errorMessage = when (e) {
                    is IOException ->
                        "Network error. Check your internet connection and try again."

                    is HttpException -> when (e.code()) {
                        400 -> "The request was invalid. Please check your message."
                        401, 403 ->
                            "The server rejected the request. Please check the configuration."
                        429 ->
                            "The service is busy. Please try again shortly."
                        in 500..599 ->
                            "The server encountered a problem. Please try again."
                        else ->
                            "Request failed (HTTP ${e.code()}). Please try again."
                    }

                    else -> e.message ?: "Something went wrong. Please try again."
                }

                if (answer.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.dropLast(1),
                        isLoading = false,
                        error = errorMessage
                    )
                } else {
                    updateAssistantMessage(
                        message = answer.toString(),
                        researchMode = currentMode,
                        sources = webSources
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }

            } finally {
                if (generation == sessionVersion) {
                    activeResearchJob = null
                }
            }
        }
    }

    private fun updateAssistantMessage(
        message: String,
        researchMode: String,
        sources: List<Source> = emptyList()
    ) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfLast { !it.isUser }

        if (index == -1) return

        currentMessages[index] = currentMessages[index].copy(
            message = message,
            sources = sources,
            researchMode = researchMode
        )

        _uiState.value = _uiState.value.copy(
            messages = currentMessages
        )
    }
}
