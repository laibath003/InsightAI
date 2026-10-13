package com.example.insightai.data.model

data class ChatResponse(
    val answer: String,
    val sources: List<Source>
)
