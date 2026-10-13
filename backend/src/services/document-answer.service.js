
import { retrieveRelevantChunks } from "./retrieval.service.js";
import { generateDocumentAnswer } from "./gemini.service.js";

const answerDocumentQuestion = async (question, documentIds) => {
    if (!question?.trim()) {
        throw new Error("Please provide a question.");
    }

    const chunks = await retrieveRelevantChunks(question, documentIds);
    const answer = await generateDocumentAnswer(question, chunks);

    return {
        answer,
        sources: chunks.map((chunk) => ({
            title: chunk.fileName,
            chunkIndex: chunk.chunkIndex,
            score: chunk.score
        }))
    };
};

export { answerDocumentQuestion };
