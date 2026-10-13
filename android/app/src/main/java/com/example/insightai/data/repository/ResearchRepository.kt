
package com.example.insightai.data.repository

import com.example.insightai.data.model.ChatRequest
import com.example.insightai.data.model.ChatResponse
import com.example.insightai.data.model.CombinedResearchRequest
import com.example.insightai.data.model.CombinedResearchResponse
import com.example.insightai.data.model.DocumentAnswerResponse
import com.example.insightai.data.model.DocumentQuestionRequest
import com.example.insightai.data.model.DocumentUploadResponse
import com.example.insightai.data.model.DocumentSource
import com.example.insightai.data.model.Source
import com.example.insightai.data.model.StreamResearchRequest
import com.example.insightai.data.remote.InsightApi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import okhttp3.MultipartBody
import org.json.JSONArray
import org.json.JSONObject

import javax.inject.Inject

class ResearchRepository @Inject constructor(
    private val api: InsightApi
) {

    suspend fun research(request: ChatRequest): ChatResponse {
        return api.research(request)
    }

    suspend fun combinedResearch(
        request: CombinedResearchRequest
    ): CombinedResearchResponse {
        return api.combinedResearch(request)
    }

    suspend fun uploadDocument(
        document: MultipartBody.Part
    ): DocumentUploadResponse {
        return api.uploadDocument(document)
    }

    suspend fun askDocument(
        documentId: String,
        question: String
    ): DocumentAnswerResponse {
        return api.askDocument(
            documentId = documentId,
            request = DocumentQuestionRequest(question = question)
        )
    }


    suspend fun streamResearch(
        message: String,
        mode: String,
        documentIds: List<String>,
        onStatus: (String) -> Unit,
        onMode: (String) -> Unit,
        onChunk: (String) -> Unit,
        onSources: (List<Source>, List<DocumentSource>) -> Unit
    ) = withContext(Dispatchers.IO) {
        api.streamResearch(
            StreamResearchRequest(
                message = message,
                mode = mode,
                documentIds = documentIds.distinct()
            )
        ).use { body ->
            val source = body.source()

            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue

                val event = JSONObject(line)

                when (event.optString("type")) {
                    "status" -> onStatus(event.optString("message"))

                    "mode" -> onMode(event.optString("mode"))

                    "chunk" -> {
                        val text = event.optString("text")
                        if (text.isNotEmpty()) onChunk(text)
                    }

                    "sources" -> onSources(
                        parseWebSources(event.optJSONArray("sources")),
                        parseDocumentSources(
                            event.optJSONArray("documentSources")
                        )
                    )

                    "error" -> throw IllegalStateException(
                        event.optString("message", "Research failed.")
                    )

                    "done" -> return@withContext
                }
            }
        }
    }


    private fun parseWebSources(array: JSONArray?): List<Source> {
        if (array == null) return emptyList()

        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                val title = item.optString("title")
                val url = item.optString("url")

                if (title.isNotBlank() && url.isNotBlank()) {
                    add(Source(title = title, url = url))
                }
            }
        }
    }

    private fun parseDocumentSources(
        array: JSONArray?
    ): List<DocumentSource> {
        if (array == null) return emptyList()

        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                add(
                    DocumentSource(
                        title = item.optString("title", "Document"),
                        chunkIndex = item.optInt("chunkIndex", 0),
                        score = item.optDouble("score", 0.0).toFloat()
                    )
                )
            }
        }
    }
}
