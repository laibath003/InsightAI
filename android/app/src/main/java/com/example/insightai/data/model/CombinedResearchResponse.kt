
package com.example.insightai.data.model

data class CombinedResearchResponse(
    val answer: String,
    val sources: List<Source> = emptyList(),
    val documentSources: List<DocumentSource> = emptyList(),
    val selectedMode: String = "BOTH"
)