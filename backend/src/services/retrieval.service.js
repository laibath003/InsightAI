
import { generateEmbedding } from "./embedding.service.js";
import { searchChunks } from "./vector.service.js";

const retrieveRelevantChunks = async (question, documentIds) => {
    if (!question?.trim()) {
        throw new Error("Please provide a question.");
    }

    const ids = [
        ...new Set(
            (Array.isArray(documentIds) ? documentIds : [documentIds])
                .filter((id) => typeof id === "string" && id.trim())
        )
    ];

    if (ids.length === 0) {
        throw new Error("Please provide at least one document ID.");
    }

    const questionEmbedding = await generateEmbedding(question);
    const chunks = await searchChunks(questionEmbedding, ids, 6);

    if (chunks.length === 0) {
        throw new Error("No matching chunks found in the uploaded PDFs.");
    }

    return chunks;
};

export { retrieveRelevantChunks };
