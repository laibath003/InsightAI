const env = {
    port: process.env.PORT || 8000,
    geminiApiKey: process.env.GEMINI_API_KEY,
    tavilyApiKey: process.env.TAVILY_API_KEY,
    qdrantUrl: process.env.QDRANT_URL,
    qdrantApiKey: process.env.QDRANT_API_KEY,
    qdrantCollection: process.env.QDRANT_COLLECTION || "insightai_documents"
};

export { env };