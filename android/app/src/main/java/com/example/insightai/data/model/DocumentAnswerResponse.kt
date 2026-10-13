package com.example.insightai.data.model

data class DocumentAnswerResponse(
    val message: String,
    val documentId: String,
    val answer: String,
    val sources: List<DocumentSource>
)