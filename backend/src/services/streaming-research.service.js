
import { GoogleGenAI } from "@google/genai";
import { env } from "../config/env.js";
import { searchWeb } from "./tavily.service.js";
import { retrieveRelevantChunks } from "./retrieval.service.js";
import { classifyResearchMode } from "./gemini.service.js";

const genAI = new GoogleGenAI({
    apiKey: env.geminiApiKey
});

const MODEL = "gemini-3.5-flash-lite";

const emitAnswer = async (prompt, emit) => {
    const retryDelays = [2000, 4000, 8000];

    for (let attempt = 0; attempt <= retryDelays.length; attempt++) {
        let emittedAnyChunk = false;

        try {
            const response = await genAI.models.generateContentStream({
                model: MODEL,
                contents: prompt
            });

            for await (const chunk of response) {
                const text = chunk.text ?? "";

                if (text.length > 0) {
                    emittedAnyChunk = true;
                    emit({
                        type: "chunk",
                        text
                    });
                }
            }

            return;
        } catch (error) {
            // Retrying after sending part of an answer would duplicate
            // or corrupt the streamed response.
            if (emittedAnyChunk || attempt === retryDelays.length) {
                throw error;
            }

            const status = error?.status ?? error?.code;
            const retryable =
                status === 429 ||
                status === 500 ||
                status === 502 ||
                status === 503 ||
                status === 504 ||
                status === "429" ||
                status === "500" ||
                status === "502" ||
                status === "503" ||
                status === "504";

            if (!retryable) {
                throw error;
            }

            await new Promise((resolve) =>
                setTimeout(resolve, retryDelays[attempt])
            );
        }
    }
};

const buildWebPrompt = (question, results) => {
    const context = results
        .map(
            (result, index) =>
                `[Source ${index + 1}]\n` +
                `Title: ${result.title}\n` +
                `URL: ${result.url}\n` +
                `Content: ${result.content ?? result.snippet ?? ""}`
        )
        .join("\n\n");

    return `
You are InsightAI, an AI research assistant.

Answer the user's question using the web research context below.
Be accurate, clear, and well organized.
Do not invent facts that are not supported by the available context.
If the context does not contain enough information, say so.
Refer to sources by their source numbers when useful.

Question:
${question}

Web research context:
${context}
`.trim();
};

const buildDocumentPrompt = (question, chunks) => {
    const context = chunks
        .map(
            (chunk, index) =>
                `[Document excerpt ${index + 1}]\n` +
                `File: ${chunk.fileName}\n` +
                `Chunk: ${chunk.chunkIndex}\n` +
                `Content: ${chunk.text}`
        )
        .join("\n\n");

    return `
You are InsightAI, an AI research assistant.

Answer the user's question using the uploaded-document excerpts below.
You may use excerpts from multiple PDFs.
Be accurate, clear, and well organized.
Do not invent information or claim a document says something unsupported.
If the excerpts do not contain enough information, explain that clearly.
Identify the relevant filenames when useful.

Question:
${question}

Uploaded-document excerpts:
${context}
`.trim();
};

const buildCombinedPrompt = (question, results, chunks) => {
    const webContext = results
        .map(
            (result, index) =>
                `[Web source ${index + 1}]\n` +
                `Title: ${result.title}\n` +
                `URL: ${result.url}\n` +
                `Content: ${result.content ?? result.snippet ?? ""}`
        )
        .join("\n\n");

    const documentContext = chunks
        .map(
            (chunk, index) =>
                `[Document excerpt ${index + 1}]\n` +
                `File: ${chunk.fileName}\n` +
                `Chunk: ${chunk.chunkIndex}\n` +
                `Content: ${chunk.text}`
        )
        .join("\n\n");

    return `
You are InsightAI, an AI research assistant.

Answer the user's question using both the web research and uploaded-document excerpts when relevant.
You may use excerpts from multiple PDFs.
Distinguish information from web sources and uploaded documents when appropriate.
Do not invent facts or claim a source supports something it does not.
If sources disagree, explain the disagreement.
If the available context is insufficient, say so.
Write a clear, well-organized answer.

Question:
${question}

Web research context:
${webContext}

Uploaded-document context:
${documentContext}
`.trim();
};

const toDocumentSources = (chunks) =>
    chunks.map((chunk) => ({
        title: chunk.fileName,
        chunkIndex: chunk.chunkIndex,
        score: chunk.score
    }));

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

const streamResearch = async (request, emit) => {
    const question = request?.message?.trim();
    const mode = request?.mode?.toUpperCase();
    const documentIds = normalizeDocumentIds(request);

    if (!question) {
        throw new Error("Please provide a question.");
    }

    if (!["WEB", "DOCUMENT", "BOTH"].includes(mode)) {
        throw new Error("Invalid research mode.");
    }

    if (mode === "DOCUMENT" && documentIds.length === 0) {
        throw new Error(
            "Please upload at least one PDF for document research."
        );
    }

    let selectedMode = mode;

    // If BOTH is selected without uploaded PDFs, use web research.
    if (selectedMode === "BOTH" && documentIds.length === 0) {
        selectedMode = "WEB";
    } else if (selectedMode === "BOTH") {
        emit({
            type: "status",
            message: "Choosing the best research mode..."
        });

        selectedMode = await classifyResearchMode(question);

        // Classification should not select document research when
        // no documents are available.
        if (
            selectedMode === "DOCUMENT" &&
            documentIds.length === 0
        ) {
            selectedMode = "WEB";
        }

        if (!["WEB", "DOCUMENT", "BOTH"].includes(selectedMode)) {
            selectedMode = "BOTH";
        }
    }

    emit({
        type: "mode",
        mode: selectedMode
    });

    if (selectedMode === "WEB") {
        emit({
            type: "status",
            message: "Searching the web..."
        });

        const results = await searchWeb(question);

        emit({
            type: "sources",
            sources: results.map((result) => ({
                title: result.title,
                url: result.url
            })),
            documentSources: []
        });

        emit({
            type: "status",
            message: "Generating answer..."
        });

        await emitAnswer(
            buildWebPrompt(question, results),
            emit
        );
    } else if (selectedMode === "DOCUMENT") {
        emit({
            type: "status",
            message: "Searching your uploaded PDFs..."
        });

        const chunks = await retrieveRelevantChunks(
            question,
            documentIds
        );

        emit({
            type: "sources",
            sources: [],
            documentSources: toDocumentSources(chunks)
        });

        emit({
            type: "status",
            message: "Generating answer from your documents..."
        });

        await emitAnswer(
            buildDocumentPrompt(question, chunks),
            emit
        );
    } else {
        emit({
            type: "status",
            message: "Searching the web and your uploaded PDFs..."
        });

        const [results, chunks] = await Promise.all([
            searchWeb(question),
            retrieveRelevantChunks(question, documentIds)
        ]);

        emit({
            type: "sources",
            sources: results.map((result) => ({
                title: result.title,
                url: result.url
            })),
            documentSources: toDocumentSources(chunks)
        });

        emit({
            type: "status",
            message: "Generating combined research answer..."
        });

        await emitAnswer(
            buildCombinedPrompt(question, results, chunks),
            emit
        );
    }

    emit({
        type: "done"
    });
};

export { streamResearch };
