
import { GoogleGenAI } from "@google/genai";
import { env } from "../config/env.js";

const ai = new GoogleGenAI({
    apiKey: env.geminiApiKey
});

const generateEmbedding = async (text) => {
    const response = await ai.models.embedContent({
        model: "gemini-embedding-001",
        contents: text,
        config: {
            outputDimensionality: 768
        }
    });

    const values = response.embeddings?.[0]?.values;

    if (!values || values.length !== 768) {
        throw new Error("Failed to generate a valid text embedding.");
    }

    return values;
};

export { generateEmbedding };