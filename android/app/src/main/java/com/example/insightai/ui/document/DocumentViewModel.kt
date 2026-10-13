
package com.example.insightai.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightai.data.model.DocumentAnswerResponse
import com.example.insightai.data.model.DocumentInfo
import com.example.insightai.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class DocumentUiState(
    val isUploading: Boolean = false,
    val isAsking: Boolean = false,
    val uploadedDocument: DocumentInfo? = null,
    val uploadedDocuments: List<DocumentInfo> = emptyList(),
    val answer: DocumentAnswerResponse? = null,
    val error: String? = null
)

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: ResearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private var uploadJob: Job? = null

    fun uploadDocument(document: MultipartBody.Part) {
        if (_uiState.value.isUploading) return

        uploadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                error = null
            )

            try {
                val response = repository.uploadDocument(document)
                val uploaded = response.document
                val currentDocuments = _uiState.value.uploadedDocuments

                val updatedDocuments =
                    if (currentDocuments.any {
                            it.documentId == uploaded.documentId
                        }
                    ) {
                        currentDocuments
                    } else {
                        currentDocuments + uploaded
                    }

                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadedDocument = uploaded,
                    uploadedDocuments = updatedDocuments,
                    answer = null,
                    error = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "Could not upload the PDF."
                )
            } finally {
                uploadJob = null
            }
        }
    }

    fun removeDocument(documentId: String) {
        if (_uiState.value.isUploading) return

        val remaining = _uiState.value.uploadedDocuments.filterNot {
            it.documentId == documentId
        }

        _uiState.value = _uiState.value.copy(
            uploadedDocuments = remaining,
            uploadedDocument = remaining.lastOrNull(),
            answer = null,
            error = null
        )
    }

    fun askQuestion(question: String) {
        val document = _uiState.value.uploadedDocument

        if (document == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please upload a PDF first."
            )
            return
        }

        if (_uiState.value.isAsking) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAsking = true,
                error = null
            )

            try {
                val answer = repository.askDocument(
                    documentId = document.documentId,
                    question = question.trim()
                )

                _uiState.value = _uiState.value.copy(
                    isAsking = false,
                    answer = answer,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAsking = false,
                    error = e.message ?: "Could not answer the question."
                )
            }
        }
    }

    fun clearDocument() {
        if (_uiState.value.isUploading) return

        _uiState.value = DocumentUiState()
    }
}
