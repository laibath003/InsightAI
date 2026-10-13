
import "dotenv/config";
import app from "./app.js";
import { ensureCollection } from "./config/qdrant.js";

const PORT = process.env.PORT || 8000;

const startServer = async () => {
    try {
        // Verify that the Qdrant collection is ready before accepting requests.
        await ensureCollection();
        console.log("Qdrant collection is ready.");

        const server = app.listen(PORT, "0.0.0.0", () => {
            console.log(`InsightAI backend running on port ${PORT}`);
        });

        // Handle errors that occur while starting or running the HTTP server.
        server.on("error", (error) => {
            console.error("HTTP server error:", error.message);
            process.exit(1);
        });
    } catch (error) {
        console.error("Failed to initialize InsightAI backend:", error.message);
        process.exit(1);
    }
};

startServer();