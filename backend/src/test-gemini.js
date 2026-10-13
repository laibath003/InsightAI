import "dotenv/config";
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

try {
    const response = await ai.models.generateContent({
        model: "gemini-3.5-flash-lite",
        contents: "Explain artificial intelligence in two sentences."
    });

    console.log(response.text);
} catch (error) {
    console.error("Gemini test failed:", error.message);
}