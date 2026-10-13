
package com.example.insightai.ui.chat

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.insightai.R
import com.example.insightai.data.model.DocumentInfo
import com.example.insightai.ui.document.DocumentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val viewModel: ChatViewModel by viewModels()
    private val documentViewModel: DocumentViewModel by viewModels()

    private lateinit var adapter: ChatAdapter
    private lateinit var modeSpinner: Spinner
    private lateinit var newChatButton: Button
    private lateinit var selectPdfButton: Button
    private lateinit var progressUpload: ProgressBar
    private lateinit var documentStatus: TextView
    private lateinit var documentListContainer: LinearLayout
    private lateinit var sendButton: Button
    private lateinit var stopButton: Button
    private lateinit var editTextMessage: EditText

    private lateinit var researchControlsContainer: LinearLayout
    private lateinit var researchModeHeader: LinearLayout
    private lateinit var researchModeToggle: TextView

    private var researchControlsExpanded = true
    private var lastShownError: String? = null

    // Uploaded filenames remain hidden until the user expands the list.
    private var documentsExpanded = false

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                uri: Uri? ->
            uri?.let { uploadPdf(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        modeSpinner = view.findViewById(R.id.spinnerResearchMode)
        newChatButton = view.findViewById(R.id.buttonNewChat)
        selectPdfButton = view.findViewById(R.id.buttonSelectPdf)
        progressUpload = view.findViewById(R.id.progressUpload)
        documentStatus = view.findViewById(R.id.textDocumentStatus)

        documentListContainer =
            view.findViewById(R.id.containerUploadedDocuments)

        researchControlsContainer =
            view.findViewById(R.id.containerResearchControls)

        researchModeHeader =
            view.findViewById(R.id.layoutResearchModeHeader)

        researchModeToggle =
            view.findViewById(R.id.textResearchModeToggle)

        // Expand or collapse the complete research controls section.
        researchModeHeader.setOnClickListener {
            researchControlsExpanded = !researchControlsExpanded

            researchControlsContainer.visibility =
                if (researchControlsExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            researchModeToggle.text =
                if (researchControlsExpanded) "▲" else "▼"
        }

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.recyclerViewMessages)

        editTextMessage = view.findViewById(R.id.editTextMessage)
        sendButton = view.findViewById(R.id.buttonSend)
        stopButton = view.findViewById(R.id.buttonStop)

        adapter = ChatAdapter(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val researchModes = listOf(
            "Web Research",
            "Document Research",
            "Web + Documents"
        )

        val modeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            researchModes
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        modeSpinner.adapter = modeAdapter
        modeSpinner.setSelection(0)

        modeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val showDocuments = position == 1 || position == 2

                    selectPdfButton.visibility =
                        if (showDocuments) View.VISIBLE else View.GONE

                    documentStatus.visibility =
                        if (showDocuments) View.VISIBLE else View.GONE

                    updateDocumentListVisibility()

                    val uploadedDocuments =
                        documentViewModel.uiState.value.uploadedDocuments

                    if (showDocuments && uploadedDocuments.isEmpty()) {
                        documentStatus.text =
                            "Select one or more PDFs to begin document research."
                    } else if (showDocuments) {
                        documentStatus.text =
                            buildDocumentStatus(uploadedDocuments.size)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        selectPdfButton.setOnClickListener {
            if (!documentViewModel.uiState.value.isUploading) {
                pdfPicker.launch(arrayOf("application/pdf"))
            }
        }

        // Independently expand or collapse the uploaded PDF filenames.
        documentStatus.setOnClickListener {
            val state = documentViewModel.uiState.value
            val uploadedDocuments = state.uploadedDocuments

            if (
                uploadedDocuments.isNotEmpty() &&
                !state.isUploading &&
                state.error == null
            ) {
                documentsExpanded = !documentsExpanded

                documentStatus.text =
                    buildDocumentStatus(uploadedDocuments.size)

                updateDocumentListVisibility()
            }
        }

        newChatButton.setOnClickListener {
            if (documentViewModel.uiState.value.isUploading) {
                Toast.makeText(
                    requireContext(),
                    "Please wait for the PDF upload to finish.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.newChat()

            documentsExpanded = false
            researchControlsExpanded = true

            researchControlsContainer.visibility = View.VISIBLE
            researchModeToggle.text = "▲"

            documentViewModel.clearDocument()

            editTextMessage.text.clear()
            editTextMessage.error = null
            modeSpinner.setSelection(0)
            lastShownError = null

            Toast.makeText(
                requireContext(),
                "New chat started.",
                Toast.LENGTH_SHORT
            ).show()
        }

        sendButton.setOnClickListener {
            val message = editTextMessage.text.toString().trim()

            if (message.isBlank()) {
                editTextMessage.error = "Please enter a question."
                return@setOnClickListener
            }

            if (viewModel.uiState.value.isLoading) {
                return@setOnClickListener
            }

            editTextMessage.error = null

            val mode = when (modeSpinner.selectedItemPosition) {
                1 -> "DOCUMENT"
                2 -> "BOTH"
                else -> "WEB"
            }

            viewModel.sendMessage(message, mode)
            editTextMessage.text.clear()
        }

        stopButton.setOnClickListener {
            viewModel.stopGenerating()
        }

        observeChat(recyclerView)
        observeDocumentState()
    }

    private fun buildDocumentStatus(documentCount: Int): String {
        return "$documentCount PDFs attached " +
                if (documentsExpanded) "▲" else "▼"
    }

    private fun updateDocumentListVisibility() {
        val showDocuments =
            modeSpinner.selectedItemPosition == 1 ||
                    modeSpinner.selectedItemPosition == 2

        val uploadedDocuments =
            documentViewModel.uiState.value.uploadedDocuments

        documentListContainer.visibility =
            if (
                showDocuments &&
                uploadedDocuments.isNotEmpty() &&
                documentsExpanded
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun uploadPdf(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                documentStatus.visibility = View.VISIBLE
                documentStatus.text = "Preparing PDF..."

                val resolver = requireContext().contentResolver

                val fileName = resolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                OpenableColumns.DISPLAY_NAME
                            )
                        )
                    } else {
                        null
                    }
                } ?: "document.pdf"

                val pdfBytes = resolver.openInputStream(uri)?.use {
                    it.readBytes()
                } ?: throw IllegalStateException(
                    "Could not open the selected PDF."
                )

                if (pdfBytes.isEmpty()) {
                    throw IllegalStateException(
                        "The selected PDF is empty."
                    )
                }

                val requestBody = pdfBytes.toRequestBody(
                    "application/pdf".toMediaType()
                )

                val part = MultipartBody.Part.createFormData(
                    "document",
                    fileName,
                    requestBody
                )

                documentViewModel.uploadDocument(part)

            } catch (e: Exception) {
                if (isAdded) {
                    documentStatus.visibility = View.VISIBLE
                    documentStatus.text =
                        "Could not prepare PDF: " +
                                (e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun observeChat(recyclerView: RecyclerView) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    adapter.submitList(state.messages)

                    sendButton.visibility =
                        if (state.isLoading) View.GONE else View.VISIBLE

                    stopButton.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    sendButton.isEnabled = !state.isLoading
                    stopButton.isEnabled = state.isLoading

                    val currentError = state.error

                    if (
                        currentError != null &&
                        currentError != lastShownError &&
                        isAdded
                    ) {
                        Toast.makeText(
                            requireContext(),
                            currentError,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    lastShownError = currentError

                    if (state.messages.isNotEmpty()) {
                        recyclerView.post {
                            if (recyclerView.isAttachedToWindow) {
                                recyclerView.scrollToPosition(
                                    adapter.itemCount - 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeDocumentState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentViewModel.uiState.collect { state ->

                    viewModel.setActiveDocuments(
                        state.uploadedDocuments.map { it.documentId }
                    )

                    progressUpload.visibility =
                        if (state.isUploading) View.VISIBLE else View.GONE

                    selectPdfButton.isEnabled = !state.isUploading
                    newChatButton.isEnabled = !state.isUploading

                    val showDocuments =
                        modeSpinner.selectedItemPosition == 1 ||
                                modeSpinner.selectedItemPosition == 2

                    documentStatus.visibility =
                        if (showDocuments) View.VISIBLE else View.GONE

                    documentStatus.text = when {
                        state.isUploading ->
                            "Uploading PDF..."

                        state.error != null ->
                            "Error: ${state.error}"

                        state.uploadedDocuments.isNotEmpty() ->
                            buildDocumentStatus(
                                state.uploadedDocuments.size
                            )

                        else ->
                            "Select one or more PDFs to begin document research."
                    }

                    documentStatus.isClickable =
                        state.uploadedDocuments.isNotEmpty() &&
                                !state.isUploading &&
                                state.error == null

                    documentStatus.alpha =
                        if (documentStatus.isClickable) 1f else 0.85f

                    renderUploadedDocuments(
                        documents = state.uploadedDocuments,
                        isUploading = state.isUploading,
                        showDocuments = showDocuments
                    )
                }
            }
        }
    }

    private fun renderUploadedDocuments(
        documents: List<DocumentInfo>,
        isUploading: Boolean,
        showDocuments: Boolean
    ) {
        documentListContainer.removeAllViews()

        documents.forEach { document ->

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val nameView = TextView(requireContext()).apply {
                text = document.fileName
                textSize = 13f
                maxLines = 2

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val removeButton = Button(requireContext()).apply {
                text = "Remove"
                isEnabled = !isUploading

                setOnClickListener {
                    documentViewModel.removeDocument(
                        document.documentId
                    )
                }
            }

            row.addView(nameView)
            row.addView(removeButton)

            documentListContainer.addView(row)
        }

        documentListContainer.visibility =
            if (
                showDocuments &&
                documents.isNotEmpty() &&
                documentsExpanded
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
