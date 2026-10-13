
import express from "express";
import cors from "cors";

import documentRoutes from "./routes/document.routes.js";
import researchRoutes from "./routes/research.routes.js";

const app = express();

// Global middleware
app.use(cors());
app.use(express.json({ limit: "1mb" }));

// Health check
app.get("/api/health", (req, res) => {
    res.json({
        status: "ok",
        message: "InsightAI backend is running"
    });
});

// API routes
app.use("/api/research", researchRoutes);
app.use("/api/documents", documentRoutes);

// Handle unknown routes
app.use((req, res) => {
    res.status(404).json({
        message: `Route ${req.method} ${req.originalUrl} not found`
    });
});

// Centralized error-handling middleware
app.use((error, req, res, next) => {
    // The response may already have been sent.
    if (res.headersSent) {
        return next(error);
    }

    let statusCode = error.status || error.statusCode || 500;
    let message = error.message;

    // Invalid JSON sent by the client
    if (error.type === "entity.parse.failed") {
        statusCode = 400;
        message = "Invalid JSON request body.";
    }

    // File upload errors from Multer
    if (error.name === "MulterError") {
        statusCode = 400;

        if (error.code === "LIMIT_FILE_SIZE") {
            message = "The uploaded file is too large.";
        } else if (error.code === "LIMIT_UNEXPECTED_FILE") {
            message = "Unexpected upload field. Use the document field.";
        } else {
            message = "The file upload failed.";
        }
    }

    // Known request-validation errors
    const validationMessages = [
        "Please provide a question.",
        "Please upload a document first."
    ];

    if (validationMessages.includes(error.message)) {
        statusCode = 400;
    }

    // Never expose internal details for server errors.
    if (statusCode >= 500) {
        console.error("Backend error:", error);
        message = "An internal server error occurred. Please try again later.";
    } else {
        // Avoid returning invalid HTTP status codes.
        if (statusCode < 400 || statusCode > 599) {
            statusCode = 500;
            message = "An internal server error occurred. Please try again later.";
            console.error("Unexpected error status:", error);
        }

        message = message || "The request could not be processed.";
    }

    res.status(statusCode).json({ message });
});

export default app;