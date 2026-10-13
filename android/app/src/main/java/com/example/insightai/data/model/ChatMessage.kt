
package com.example.insightai.data.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val sources: List<Source> = emptyList(),
    val researchMode: String? = null
)