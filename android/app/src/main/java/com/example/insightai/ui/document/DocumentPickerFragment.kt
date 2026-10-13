package com.example.insightai.ui.document

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.insightai.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@AndroidEntryPoint
class DocumentPickerFragment : Fragment(R.layout.fragment_document_picker) {

    private val viewModel: DocumentViewModel by viewModels()

    private lateinit var selectPdfButton: Button
    private lateinit var uploadProgress: ProgressBar
    private lateinit var documentStatus: TextView

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { uploadPdf(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectPdfButton = view.findViewById(R.id.selectPdfButton)
        uploadProgress = view.findViewById(R.id.uploadProgress)
        documentStatus = view.findViewById(R.id.documentStatus)

        selectPdfButton.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    uploadProgress.visibility =
                        if (state.isUploading) View.VISIBLE else View.GONE

                    selectPdfButton.isEnabled = !state.isUploading

                    documentStatus.text = when {
                        state.isUploading -> "Uploading PDF..."
                        state.error != null -> "Error: ${state.error}"
                        state.uploadedDocument != null -> {
                            val document = state.uploadedDocument
                            "PDF uploaded successfully!\n\n" +
                                    "File: ${document.fileName}\n" +
                                    "Characters: ${document.characterCount}\n" +
                                    "Chunks: ${document.chunkCount}\n" +
                                    "Stored chunks: ${document.storedChunkCount}\n\n" +
                                    "You can now ask questions about this document."
                        }
                        else -> "Select a PDF to upload."
                    }
                }
            }
        }
    }

    private fun uploadPdf(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                documentStatus.text = "Preparing PDF..."

                val contentResolver = requireContext().contentResolver
                val fileName = queryFileName(uri) ?: "document.pdf"
                val tempFile = File.createTempFile(
                    "insightai_",
                    ".pdf",
                    requireContext().cacheDir
                )

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open selected PDF.")

                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val requestBody = tempFile.asRequestBody(
                        "application/pdf".toMediaType()
                    )

                    val part = MultipartBody.Part.createFormData(
                        "document",
                        fileName,
                        requestBody
                    )

                    viewModel.uploadDocument(part)
                } finally {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                documentStatus.text =
                    "Could not prepare PDF: ${e.message ?: "Unknown error"}"
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(
                    it.getColumnIndexOrThrow(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                )
            } else {
                null
            }
        }
    }
}