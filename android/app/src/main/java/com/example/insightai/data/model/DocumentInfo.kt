package com.example.insightai.data.model
data class DocumentInfo(
    val documentId: String,
    val fileName: String,
    val characterCount: Int,
    val chunkCount: Int,
    val storedChunkCount: Int
)