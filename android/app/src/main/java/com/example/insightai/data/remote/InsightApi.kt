
package com.example.insightai.data.remote

import com.example.insightai.data.model.ChatRequest
import com.example.insightai.data.model.ChatResponse
import com.example.insightai.data.model.CombinedResearchRequest
import com.example.insightai.data.model.CombinedResearchResponse
import com.example.insightai.data.model.DocumentAnswerResponse
import com.example.insightai.data.model.DocumentQuestionRequest
import com.example.insightai.data.model.DocumentUploadResponse
import com.example.insightai.data.model.StreamResearchRequest

import okhttp3.MultipartBody
import okhttp3.ResponseBody

import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Streaming

interface InsightApi {

    @POST("api/research")
    suspend fun research(
        @Body request: ChatRequest
    ): ChatResponse

    @POST("api/research/both")
    suspend fun combinedResearch(
        @Body request: CombinedResearchRequest
    ): CombinedResearchResponse

    @Streaming
    @POST("api/research/stream")
    suspend fun streamResearch(
        @Body request: StreamResearchRequest
    ): ResponseBody

    @Multipart
    @POST("api/documents/upload")
    suspend fun uploadDocument(
        @Part document: MultipartBody.Part
    ): DocumentUploadResponse

    @POST("api/documents/{documentId}/ask")
    suspend fun askDocument(
        @Path("documentId") documentId: String,
        @Body request: DocumentQuestionRequest
    ): DocumentAnswerResponse
}
