import { GoogleGenAI } from "@google/genai";
import { env } from "../config/env.js";

const ai = new GoogleGenAI({
    apiKey: env.geminiApiKey
});

const wait = (ms) =>
    new Promise((resolve) => setTimeout(resolve, ms));

const generateWithRetry = async (prompt) => {
    const delays = [2000, 4000, 8000];

    for (let attempt = 0; ; attempt++) {
        try {
            return await ai.models.generateContent({
                model: "gemini-3.5-flash-lite",
                contents: prompt
            });
        } catch (error) {
            const isTemporary =
                error.status === 503 ||
                error.status === 429 ||
                error.status >= 500;

            if (!isTemporary || attempt >= delays.length) {
                throw error;
            }

            console.log(
                `Gemini temporarily unavailable. Retrying in ${
                    delays[attempt] / 1000
                } seconds...`
            );

            await wait(delays[attempt]);
        }
    }
};

// Decide which research sources are appropriate for a question.
const classifyResearchMode = async (question) => {
    const prompt = `
You are the research-mode classifier for InsightAI.

Choose the best research strategy for the user's question.

Available strategies:

WEB:
Choose this when the question primarily needs current information,
recent developments, news, external facts, or information from the internet.

DOCUMENT:
Choose this when the question can be answered primarily from the
uploaded document, such as summarizing it, explaining a section,
extracting information, or asking about its specific contents.

BOTH:
Choose this when answering well requires both the uploaded document
and external web information. Examples include comparing document
findings with recent research, verifying whether document claims
are still current, or combining document evidence with current facts.

Rules:
- Classify according to the user's actual intent, not just keywords.
- If the question refers to "my document", "the PDF", or a specific
  section or finding in the uploaded file, normally choose DOCUMENT.
- If the question asks to compare, verify, or update document findings
  using external information, choose BOTH.
- If the question needs current external information but does not
  depend on the uploaded document, choose WEB.
- When uncertain, choose BOTH because a document is available.
- Return exactly one word: WEB, DOCUMENT, or BOTH.
- Do not include explanations or punctuation.

USER QUESTION:
${question}
`;

    const response = await generateWithRetry(prompt);
    const result = response.text?.trim().toUpperCase();

    if (["WEB", "DOCUMENT", "BOTH"].includes(result)) {
        return result;
    }

    // Safe default when the classifier returns an unexpected response.
    return "BOTH";
};

const generateResearchAnswer = async (question, searchResults) => {
    const sourcesText = searchResults
        .map(
            (result, index) =>
                `Source ${index + 1}:
Title: ${result.title}
URL: ${result.url}
Content: ${result.content}`
        )
        .join("\n\n");

    const prompt = `
You are InsightAI, an AI research assistant.

Answer the user's question using the web search results below.

USER QUESTION:
${question}

WEB SEARCH RESULTS:
${sourcesText || "No web search results were found."}

INSTRUCTIONS:
- Give a clear and useful answer.
- Base factual claims on the provided results.
- Do not invent unsupported facts.
- Use Markdown formatting where helpful.
- Keep the answer reasonably concise.
- If the results are insufficient, acknowledge the limitation.
`;

    const response = await generateWithRetry(prompt);
    const answer = response.text?.trim();

    if (!answer) {
        throw new Error("Gemini returned an empty research answer.");
    }

    return answer;
};

const generateDocumentAnswer = async (question, chunks) => {
    const context = chunks
        .map(
            (chunk, index) =>
                `[Passage ${index + 1}]\n${chunk.text}`
        )
        .join("\n\n");

    const prompt = `
You are InsightAI, an AI research assistant.

Answer the user's question using only the provided document passages.

RULES:
- Base factual claims on the supplied passages.
- Do not invent facts or introduce unsupported information.
- If the passages do not contain enough information, clearly say so.
- Give a clear, useful answer in Markdown.
- Do not claim to have read parts of the document that were not provided.

DOCUMENT PASSAGES:
${context || "No relevant document passages were found."}

USER QUESTION:
${question}
`;

    const response = await generateWithRetry(prompt);
    const answer = response.text?.trim();

    if (!answer) {
        throw new Error("Gemini returned an empty document answer.");
    }

    return answer;
};

const generateCombinedResearchAnswer = async (
    question,
    searchResults,
    chunks
) => {
    const webContext = searchResults
        .map(
            (result, index) =>
                `[Web Source ${index + 1}]
Title: ${result.title}
URL: ${result.url}
Content: ${result.content}`
        )
        .join("\n\n");

    const documentContext = chunks
        .map(
            (chunk, index) =>
                `[Document Passage ${index + 1}]
File: ${chunk.fileName}
Section: ${(chunk.chunkIndex ?? index) + 1}
Content: ${chunk.text}`
        )
        .join("\n\n");

    const prompt = `
You are InsightAI, an AI research assistant.

Synthesize the evidence from the web results and uploaded document
passages into ONE coherent answer to the user's question.

USER QUESTION:
${question}

WEB SEARCH EVIDENCE:
${webContext || "No web evidence was retrieved."}

UPLOADED DOCUMENT EVIDENCE:
${documentContext || "No relevant document passages were retrieved."}

INSTRUCTIONS:
- Produce one synthesized answer, not two separate answers.
- Combine relevant evidence where appropriate.
- Distinguish document findings from web findings when useful.
- Identify meaningful agreements or contradictions between sources.
- Do not invent facts or make unsupported claims.
- Acknowledge insufficient evidence where necessary.
- Do not fabricate URLs or citations.
- Use clear headings or bullets when helpful.
- Return the answer in readable Markdown.

Return only the answer.
`;

    const response = await generateWithRetry(prompt);
    const answer = response.text?.trim();

    if (!answer) {
        throw new Error(
            "Gemini returned an empty combined research answer."
        );
    }

    return answer;
};

export {
    classifyResearchMode,
    generateResearchAnswer,
    generateDocumentAnswer,
    generateCombinedResearchAnswer
};

