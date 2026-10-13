
package com.example.insightai.data.model

data class StreamResearchRequest(
    val message: String,
    val mode: String,
    val documentIds: List<String> = emptyList(),

    // Kept for backward compatibility with older requests.
    val documentId: String? = null
)
