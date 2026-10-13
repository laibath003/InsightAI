
import { searchWeb } from "./tavily.service.js";
import { retrieveRelevantChunks } from "./retrieval.service.js";
import { answerDocumentQuestion } from "./document-answer.service.js";
import {
    classifyResearchMode,
    generateResearchAnswer,
    generateCombinedResearchAnswer
} from "./gemini.service.js";

const normalizeDocumentIds = (request) => {
    const suppliedIds = Array.isArray(request?.documentIds)
        ? request.documentIds
        : request?.documentId
            ? [request.documentId]
            : [];

    return [
        ...new Set(
            suppliedIds.filter(
                (id) => typeof id === "string" && id.trim().length > 0
            )
        )
    ];
};

// Web-only research.
const research = async (request) => {
    const message = request?.message?.trim();

    if (!message) {
        throw new Error("Please provide a question.");
    }

    const results = await searchWeb(message);
    const answer = await generateResearchAnswer(message, results);

    return {
        answer,
        sources: results.map((result) => ({
            title: result.title,
            url: result.url
        }))
    };
};

// Research using uploaded PDFs, web sources, or both.
const combinedResearch = async (request) => {
    const message = request?.message?.trim();
    const documentIds = normalizeDocumentIds(request);
    const requestedMode = request?.mode?.toUpperCase() || "BOTH";

    if (!message) {
        throw new Error("Please provide a question.");
    }

    if (!["WEB", "DOCUMENT", "BOTH"].includes(requestedMode)) {
        throw new Error("Invalid research mode.");
    }

    if (requestedMode === "DOCUMENT" && documentIds.length === 0) {
        throw new Error(
            "Please upload at least one PDF for document research."
        );
    }

    // BOTH without documents falls back to web research.
    let mode = requestedMode;

    if (mode === "BOTH" && documentIds.length === 0) {
        mode = "WEB";
    } else if (mode === "BOTH") {
        mode = await classifyResearchMode(message);

        if (!["WEB", "DOCUMENT", "BOTH"].includes(mode)) {
            mode = "BOTH";
        }

        if (mode === "DOCUMENT" && documentIds.length === 0) {
            mode = "WEB";
        }
    }

    if (mode === "WEB") {
        const results = await searchWeb(message);
        const answer = await generateResearchAnswer(message, results);

        return {
            mode: "WEB",
            answer,
            sources: results.map((result) => ({
                title: result.title,
                url: result.url
            })),
            documentSources: []
        };
    }

    if (mode === "DOCUMENT") {
        const result = await answerDocumentQuestion(message, documentIds);

        return {
            mode: "DOCUMENT",
            answer: result.answer,
            sources: [],
            documentSources: result.sources
        };
    }

    const [results, chunks] = await Promise.all([
        searchWeb(message),
        retrieveRelevantChunks(message, documentIds)
    ]);

    const answer = await generateCombinedResearchAnswer(
        message,
        results,
        chunks
    );

    return {
        mode: "BOTH",
        answer,
        sources: results.map((result) => ({
            title: result.title,
            url: result.url
        })),
        documentSources: chunks.map((chunk) => ({
            title: chunk.fileName,
            chunkIndex: chunk.chunkIndex,
            score: chunk.score
        }))
    };
};

export { research, combinedResearch };
