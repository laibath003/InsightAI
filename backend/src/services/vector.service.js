
import { randomUUID } from "node:crypto";
import { qdrantClient } from "../config/qdrant.js";
import { env } from "../config/env.js";

const storeChunks = async (documentId, fileName, chunks, embeddings) => {
    const points = chunks.map((chunk, index) => ({
        id: randomUUID(),
        vector: embeddings[index],
        payload: {
            documentId,
            fileName,
            chunkIndex: index,
            text: chunk
        }
    }));

    await qdrantClient.upsert(env.qdrantCollection, {
        wait: true,
        points
    });

    return points.length;
};



const searchChunks = async (questionEmbedding, documentIds, limit = 6) => {
    const ids = [
        ...new Set(
            (Array.isArray(documentIds) ? documentIds : [documentIds])
                .filter((id) => typeof id === "string" && id.trim())
        )
    ];

    if (ids.length === 0) {
        throw new Error("Please provide at least one document ID.");
    }

    try {
        const results = await qdrantClient.search(env.qdrantCollection, {
            vector: questionEmbedding,
            limit,
            with_payload: true,
            filter: {
                should: ids.map((id) => ({
                    key: "documentId",
                    match: { value: id }
                }))
            }
        });

        return results.map((result) => ({
            text: result.payload?.text ?? "",
            fileName: result.payload?.fileName ?? "",
            chunkIndex: result.payload?.chunkIndex ?? 0,
            score: result.score
        }));
    } catch (error) {
        console.error("Qdrant document search failed:", error);
        throw error;
    }
};

export { storeChunks, searchChunks };