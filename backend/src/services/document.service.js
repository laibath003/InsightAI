
import { randomUUID } from "node:crypto";
import { extractText } from "../utils/extractText.js";
import { chunkText } from "../utils/chunkText.js";
import { generateEmbedding } from "./embedding.service.js";
import { storeChunks } from "./vector.service.js";

const processDocument = async (file) => {
    if (!file) {
        throw new Error("Please upload a PDF file.");
    }

    const text = await extractText(file.buffer);
    const chunks = chunkText(text);

    if (chunks.length === 0) {
        throw new Error("No readable text chunks were created.");
    }

    const documentId = randomUUID();
    const embeddings = [];

    // Generate an embedding for each chunk.
    for (const chunk of chunks) {
        const embedding = await generateEmbedding(chunk);
        embeddings.push(embedding);
    }

    // Store the chunks and their embeddings in Qdrant.
    const storedChunkCount = await storeChunks(
        documentId,
        file.originalname,
        chunks,
        embeddings
    );

    return {
        documentId,
        fileName: file.originalname,
        characterCount: text.length,
        chunkCount: chunks.length,
        storedChunkCount
    };
};

export { processDocument };