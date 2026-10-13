
import { processDocument } from "../services/document.service.js";
import { retrieveRelevantChunks } from "../services/retrieval.service.js";
import { answerDocumentQuestion } from "../services/document-answer.service.js";

const uploadDocument = async (req, res, next) => {
    try {
        const result = await processDocument(req.file);

        res.json({
            message: "PDF processed and stored successfully.",
            document: {
                documentId: result.documentId,
                fileName: result.fileName,
                characterCount: result.characterCount,
                chunkCount: result.chunkCount,
                storedChunkCount: result.storedChunkCount
            }
        });
    } catch (error) {
        next(error);
    }
};

const searchDocument = async (req, res, next) => {
    try {
        const { question } = req.body;
        const { documentId } = req.params;

        const chunks = await retrieveRelevantChunks(
            question,
            documentId
        );

        res.json({
            message: "Relevant document chunks retrieved successfully.",
            documentId,
            results: chunks
        });
    } catch (error) {
        next(error);
    }
};

const askDocument = async (req, res, next) => {
    try {
        const { question } = req.body;
        const { documentId } = req.params;

        const result = await answerDocumentQuestion(
            question,
            documentId
        );

        res.json({
            message: "Document question answered successfully.",
            documentId,
            answer: result.answer,
            sources: result.sources
        });
    } catch (error) {
        next(error);
    }
};

export { uploadDocument, searchDocument, askDocument };